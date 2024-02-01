package xyz.ldqc.togr.server.core.tcp.support.tunnel;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.buffer.SimpleByteData;

/**
 * @author Fetters
 */
public class IntranetClientExecutor implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(IntranetClientExecutor.class);

  private static final int PREFIX_LENGTH = 8 + 1;

  private static final byte[] CLOSE_FLAG = "cls".getBytes(StandardCharsets.UTF_8);

  private static final int CLOSE_FLAG_LEN = CLOSE_FLAG.length;

  private Socket intranetClient;
  private final Map<Long, SocketChannel> idIndexRequestClient;

  private final ExecutorService executorService;

  private final ByteBuffer byteBuffer = ByteBuffer.allocate(127);

  private final ServerTunnel serverTunnel;

  public IntranetClientExecutor(Socket intranetClient,
      Map<Long, SocketChannel> idIndexRequestClient, ServerTunnel serverTunnel) {
    this.intranetClient = intranetClient;
    this.idIndexRequestClient = idIndexRequestClient;
    this.serverTunnel = serverTunnel;
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
      serverTunnel.forceClose();
      log.error("Exchange fail, {}", e.getMessage(), e);
    }
  }

  private void doExchange() throws IOException {
    byte[] buffer = new byte[4096];
    int bytesRead;
    InputStream inputStream = this.intranetClient.getInputStream();

    while ((bytesRead = inputStream.read(buffer)) != -1) {
      log.debug("Read {} bytes", bytesRead);
      SimpleByteData byteData = new SimpleByteData();
      byteData.writeBytes(buffer, 0, bytesRead);
      long id = byteData.readLong();
      byte len = byteData.readByte();
      SocketChannel socketChannel = idIndexRequestClient.get(id);
      if (len == CLOSE_FLAG_LEN && isCloseFlag(buffer)){
        socketChannel.close();
        return;
      }
      writeBackToRequestClient(byteData.readBytes(), socketChannel);
    }
  }

  private boolean isCloseFlag(byte[] buffer) {
    return Arrays.equals(buffer, PREFIX_LENGTH, PREFIX_LENGTH + CLOSE_FLAG_LEN,
        CLOSE_FLAG, 0, CLOSE_FLAG_LEN);
  }

  private void writeBackToRequestClient(byte[] data, SocketChannel reqClient) throws IOException {
    byteBuffer.clear();
    byteBuffer.put(data );
    byteBuffer.flip();
    reqClient.write(byteBuffer);
  }

  public void refreshClient(Socket intranetClient) {
    this.intranetClient = intranetClient;
  }

  public void execute() {
    executorService.execute(this);
  }
}
