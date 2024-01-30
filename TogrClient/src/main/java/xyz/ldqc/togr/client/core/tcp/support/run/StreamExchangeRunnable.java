package xyz.ldqc.togr.client.core.tcp.support.run;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author Fetters
 */
public class StreamExchangeRunnable implements Runnable {

  private final Socket socket;

  private final SocketChannel channel;

  private final ByteBuffer byteBuffer;

  public StreamExchangeRunnable(Socket socket, SocketChannel channel){
    this.socket = socket;
    this.channel = channel;
    byteBuffer = ByteBuffer.allocate(4096);
  }

  @Override
  public void run() {
    try {
      byte[] buffer = new byte[4096];
      int bytesRead;
      InputStream inputStream = this.socket.getInputStream();
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        byteBuffer.clear();
        byteBuffer.put(buffer,0, bytesRead);
        byteBuffer.flip();
        channel.write(byteBuffer);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
