package xyz.ldqc.togr.client.core.tcp.support.chain;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.togr.client.core.entity.SendFrame;

/**
 * @author Fetters
 */
public class PostHandleChain implements InboundChain, ChannelHandler {

  private static final Logger log = LoggerFactory.getLogger(PostHandleChain.class);


  @Override
  public void doChain(Channel channel, Object o) {
    if (!SendFrame.class.isAssignableFrom(o.getClass())) {
      return;
    }
    doHandler(channel, o);
  }

  @Override
  public void setNextChain(Chain chain) {
    // 没有下一个chain
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
      log.error("Send to target fail", e);
    }
  }
}
