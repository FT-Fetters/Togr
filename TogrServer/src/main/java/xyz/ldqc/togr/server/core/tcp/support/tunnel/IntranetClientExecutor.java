package xyz.ldqc.togr.server.core.tcp.support.tunnel;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.togr.server.core.entity.FrameEntity;
import xyz.ldqc.togr.server.core.tcp.support.cache.NetTransferCache;

/**
 * @author Fetters
 */
public class IntranetClientExecutor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(IntranetClientExecutor.class);

    private static final int PREFIX_LENGTH = 8;

    private static final byte[] CLOSE_FLAG = "cls".getBytes(StandardCharsets.UTF_8);

    private static final int CLOSE_FLAG_LEN = CLOSE_FLAG.length;

    private static final String PIPE_BREAK_MSG = "Broken pipe";

    private Socket intranetClient;
    private final Map<Integer, SocketChannel> idIndexRequestClient;

    private final ExecutorService executorService;

    private final ByteBuffer byteBuffer = ByteBuffer.allocate(4096 + 8);

    private final ServerTunnel serverTunnel;

    private final NetTransferCache netTransferCache;

    public IntranetClientExecutor(Socket intranetClient,
        Map<Integer, SocketChannel> idIndexRequestClient, ServerTunnel serverTunnel) {
        this.intranetClient = intranetClient;
        this.idIndexRequestClient = idIndexRequestClient;
        this.serverTunnel = serverTunnel;
        this.netTransferCache = new NetTransferCache();
        this.executorService = new ThreadPoolExecutor(
            1, 1, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), r -> new Thread(r, "Intranet-client-exchange"),
            new AbortPolicy()
        );
    }

    @Override
    public void run() {
        try {
            doExchange();
        } catch (IOException e) {
            if (PIPE_BREAK_MSG.equals(e.getMessage())) {
                run();
                return;
            }
            serverTunnel.forceClose();
            log.error("Exchange fail, {}", e.getMessage(), e);
        }
    }

    private void doExchange() throws IOException {
        byte[] buffer = new byte[4096 + 8];
        int bytesRead;
        InputStream inputStream = this.intranetClient.getInputStream();

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            List<FrameEntity> frameEntities = doRead(bytesRead, buffer);
            for (FrameEntity frameEntity : frameEntities) {
                int id = frameEntity.getId();
                byte[] data = frameEntity.getData();
                SocketChannel socketChannel = idIndexRequestClient.get(id);
                if (data.length == CLOSE_FLAG_LEN && isCloseFlag(buffer)) {
                    socketChannel.close();
                    log.info("request client {} close", socketChannel);
                    continue;
                }
                writeBackToRequestClient(data, socketChannel);
            }
        }
    }

    private List<FrameEntity> doRead(int bytesRead, byte[] buffer) {
        SimpleByteData byteData = new SimpleByteData();
        for (int i = 0; i < bytesRead; i++) {
            byteData.writeByte(buffer[i]);
        }
        netTransferCache.append(byteData.readBytes());
        return netTransferCache.consume();
    }

    private boolean isCloseFlag(byte[] buffer) {
        return Arrays.equals(buffer, PREFIX_LENGTH, PREFIX_LENGTH + CLOSE_FLAG_LEN,
            CLOSE_FLAG, 0, CLOSE_FLAG_LEN);
    }

    private void writeBackToRequestClient(byte[] data, SocketChannel reqClient) throws IOException {
        byteBuffer.clear();
        byteBuffer.put(data);
        byteBuffer.flip();
        if (reqClient == null) {
            log.debug("null");
            return;
        }
        doWriteBackToRequestClient(reqClient);

    }

    private void doWriteBackToRequestClient(SocketChannel reqClient) throws IOException {
        int remaining = byteBuffer.remaining();

        while (remaining > 0) {
            int write = reqClient.write(byteBuffer);
            remaining -= write;
        }
    }

    public void refreshClient(Socket intranetClient) {
        this.intranetClient = intranetClient;
    }

    public void execute() {
        executorService.execute(this);
    }
}
