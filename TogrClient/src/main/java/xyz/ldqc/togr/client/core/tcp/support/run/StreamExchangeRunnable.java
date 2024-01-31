package xyz.ldqc.togr.client.core.tcp.support.run;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.util.DigestUtil;

/**
 * @author Fetters
 */
public class StreamExchangeRunnable implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(StreamExchangeRunnable.class);

  /**
   * 连接目标服务器的客户端
   */
  private final Socket socket;

  /**
   * 公网服务器的通道
   */
  private final SocketChannel channel;

  private final ByteBuffer byteBuffer;

  private final long id;

  public StreamExchangeRunnable(Socket socket, long id, SocketChannel channel) {
    this.socket = socket;
    this.channel = channel;
    this.id = id;
    this.byteBuffer = ByteBuffer.allocate(127 + 8 + 1);
  }

  @Override
  public void run() {
    try {
      byte[] buffer = new byte[127];
      int bytesRead;
      InputStream inputStream = this.socket.getInputStream();
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        // 从目标服务器返回的数据
        log.debug("Read {} bytes from target", bytesRead);
        String content = new String(buffer, 0, bytesRead);
        log.debug("Read content: {}", content);
        byteBuffer.clear();
        byteBuffer.put(DigestUtil.long2byte(id));
        byteBuffer.put(((byte) bytesRead));
        byteBuffer.put(buffer, 0, bytesRead);
        byteBuffer.flip();
        channel.write(byteBuffer);
      }
      log.info("Client {} closed", socket.getInetAddress());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
