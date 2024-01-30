package xyz.ldqc.togr.client.core.tcp.support.chain;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.Channel;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.chain.OutboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.togr.client.core.entity.SendFrame;

/**
 * @author Fetters
 */
public class PostHandleChain implements InboundChain, ChannelHandler {

  private Chain nextChain;

  @Override
  public void doChain(Channel channel, Object o) {
    if (!SendFrame.class.isAssignableFrom(o.getClass())) {
      return;
    }
    doHandler(channel, o);
  }

  @Override
  public void setNextChain(Chain chain) {
    this.nextChain = chain;
  }

  @Override
  public void doHandler(Channel channel, Object o) {
    SendFrame sendFrame = (SendFrame) o;
    Socket socket = sendFrame.getSocket();
    byte[] data = sendFrame.getData();
    try {
      OutputStream outputStream = socket.getOutputStream();
      outputStream.write(data);
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
