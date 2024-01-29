import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import xyz.ldqc.tightcall.buffer.AbstractByteData;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.chain.support.DefaultChannelChainGroup;
import xyz.ldqc.tightcall.client.exce.support.NioClientExec;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;

public class EasyTest {

  @Test
  public void recTest(){
    NioClientExec nioClientExec = new NioClientExec(new InetSocketAddress("127.0.0.1", 8888));
    nioClientExec.setChainGroup(new DefaultChannelChainGroup().addLast(new TChain()));
    nioClientExec.start();
    LockSupport.park();
  }


  private class TChain implements ChannelHandler, InboundChain {

    private Chain nextChain;

    private Socket target;

    private SocketChannel channel;


    public TChain(){
      try {
        this.target = new Socket("127.0.0.1", 8771);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      new Thread(
          () -> {
            try {
              InputStream inputStream = target.getInputStream();
              byte[] buffer = new byte[4096];
              int bytesRead;

              while ((bytesRead = inputStream.read(buffer)) != -1) {
                ByteBuffer buf = ByteBuffer.allocate(bytesRead);
                buf.clear();
                buf.put(buffer,0, bytesRead);
                buf.flip();
                System.out.println(new String(buffer));
                int write = this.channel.write(buf);
                System.out.println("write len:" + write);
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
      ).start();
    }

    @Override
    public void doHandler(Channel channel, Object o) {

    }

    @Override
    public void doChain(Channel channel, Object o) {
      // 判断是否是SocketChannel
      if (!(channel instanceof SocketChannel)) {
        return;
      }
      if (!(o instanceof SelectionKey)){
        throw new RuntimeException("obj must be selection key");
      }
      SelectionKey selectionKey = (SelectionKey) o;
      SocketChannel socketChannel = ((SocketChannel) channel);
      this.channel = socketChannel;
      AbstractByteData abstractByteData = readDataFromChanel(socketChannel);


      if (abstractByteData == null){
        selectionKey.cancel();
        return;
      }
      byte[] clientId = abstractByteData.readBytes(8);
      byte len = abstractByteData.readByte();

      byte[] data = abstractByteData.readBytes();

      try {
        OutputStream outputStream = this.target.getOutputStream();
        outputStream.write(data);
        outputStream.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }


    }

    protected AbstractByteData readDataFromChanel(SocketChannel socketChannel){
      ByteBuffer buffer = ByteBuffer.allocate(127 + 8 + 1);
      try {
        int readLen = socketChannel.read(buffer);
        if ( readLen == -1) {
          return null;
        }
        if (readLen == 0){
          return new SimpleByteData();
        }
        return new SimpleByteData(buffer);
      } catch (IOException e) {
        return null;
      }
    }

    @Override
    public void setNextChain(Chain chain) {
      this.nextChain = chain;
    }
  }

}
