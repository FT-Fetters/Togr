package xyz.ldqc.togr.client.core.tcp.support.chain;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.togr.client.core.entity.DataFrame;
import xyz.ldqc.togr.client.core.entity.SendFrame;
import xyz.ldqc.togr.client.core.tcp.support.run.StreamExchangeRunnable;
import xyz.ldqc.togr.client.exception.HandleDataFrameException;

/**
 * @author Fetters
 */
public class HandleDataFrameChain implements ChannelHandler, InboundChain {

    private static final Logger log = LoggerFactory.getLogger(HandleDataFrameChain.class);

    private static final String LOCAL_HOST = "127.0.0.1";

    private static final byte[] CLOSE_FLAG = "cls".getBytes(StandardCharsets.UTF_8);

    private static final byte[] MAGIC_NUM = "TOGR".getBytes(StandardCharsets.UTF_8);

    private final String ip;

    private final int port;

    private Chain nextChain;

    private final Map<Integer, Socket> socketMap = new HashMap<>();

    private final ExecutorService socketStreamPool;

    private final AtomicInteger exchangeId = new AtomicInteger(0);

    public HandleDataFrameChain(int port) {
        this(LOCAL_HOST, port);
    }

    public HandleDataFrameChain(String ip, int port) {
        this.ip = ip;
        this.port = port;
        int cpu = Runtime.getRuntime().availableProcessors();
        socketStreamPool = new ThreadPoolExecutor(
            cpu * 2, Integer.MAX_VALUE, Integer.MAX_VALUE, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128), r -> new Thread(r, "stream-exchange-" + exchangeId.getAndIncrement())
        );
    }


    @Override
    public void doChain(Channel channel, Object o) {
        doHandler(channel, o);
    }

    @Override
    public void setNextChain(Chain chain) {
        this.nextChain = chain;
    }

    @Override
    public void doHandler(Channel channel, Object o) {
        if (!DataFrame.class.isAssignableFrom(o.getClass())) {
            return;
        }
        DataFrame dataFrame = (DataFrame) o;

        if (
            dataFrame.getData().length == CLOSE_FLAG.length &&
                (Arrays.equals(dataFrame.getData(), CLOSE_FLAG))
        ) {
            int id = dataFrame.getId();
            try {
                Socket remove = socketMap.remove(id);
                remove.close();
            } catch (IOException e) {
                log.error("Close fail", e);
            }
            return;
        }

        int id = dataFrame.getId();
        byte[] data = dataFrame.getData();
        Socket socket = socketMap.get(id);
        socket = checkTargetAvailable(socket, (SocketChannel) channel, id);
        if (socket == null) {
            return;
        }
        this.socketMap.put(id, socket);
        SendFrame sendFrame = new SendFrame(socket, data);
        nextChain.doChain(channel, sendFrame);
    }


    private Socket checkTargetAvailable(Socket socket, SocketChannel channel, int id) {
        if (socket == null || checkTargetIsShutdown(socket)) {
            CountDownLatch connectLock = new CountDownLatch(1);
            try {
                Socket newSocket = new Socket();
                this.socketStreamPool.execute(
                    new StreamExchangeRunnable(newSocket, id, channel, connectLock)
                );
                newSocket.connect(new InetSocketAddress(this.ip, this.port));
                return newSocket;
            } catch (IOException e) {
                SimpleByteData byteData = new SimpleByteData();
                byteData.writeBytes(MAGIC_NUM).writeBytes(int2TwoByte(id))
                    .writeBytes(int2TwoByte(CLOSE_FLAG.length))
                    .writeBytes("cls".getBytes(StandardCharsets.UTF_8));
                ByteBuffer buffer = ByteBuffer.allocate(byteData.remaining());
                buffer.clear();
                buffer.put(byteData.readBytes());
                buffer.flip();
                try {
                    channel.write(buffer);
                } catch (IOException ex) {
                    log.error("Send close flag fail", ex);
                }
                log.error("Connect fail, {}", e.getMessage());
                return null;
            }finally {
                connectLock.countDown();
            }
        } else {
            return socket;
        }

    }

    private byte[] int2TwoByte(int i) {
        byte[] bytes = new byte[2];
        int b0 = (i >> 8) - 128;
        int b1 = (i & 0xFF) - 128;
        bytes[0] = ((byte) b0);
        bytes[1] = ((byte) b1);
        return bytes;
    }

    private boolean checkTargetIsShutdown(Socket socket) {
        return !socket.isConnected() || socket.isInputShutdown() || socket.isOutputShutdown();
    }
}
