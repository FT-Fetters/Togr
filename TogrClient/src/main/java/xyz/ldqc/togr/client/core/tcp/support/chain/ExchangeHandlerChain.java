package xyz.ldqc.togr.client.core.tcp.support.chain;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import xyz.ldqc.tightcall.buffer.AbstractByteData;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.togr.client.core.entity.DataFrame;
import xyz.ldqc.togr.client.exception.ExchangeHandlerException;

/**
 * @author Fetters
 */
public class ExchangeHandlerChain implements ChannelHandler, InboundChain {


  private Chain nextChain;

  private Socket target;

  private final int targetPort;

  public ExchangeHandlerChain(int port) {
    this.targetPort = port;
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
    // 判断是否是SocketChannel
    if (!(channel instanceof SocketChannel)) {
      return;
    }
    if (!(o instanceof SelectionKey)) {
      return;
    }
    SelectionKey selectionKey = (SelectionKey) o;
    SocketChannel socketChannel = ((SocketChannel) channel);
    AbstractByteData byteData = readDataFromChanel(socketChannel);
    if (byteData == null){
      selectionKey.cancel();
      return;
    }
    DataFrame dataFrame = readDataFrame(byteData);
    nextChain.doChain(channel, dataFrame);
  }

  private DataFrame readDataFrame(AbstractByteData byteData){
    long id = byteData.readLong();
    byte len = byteData.readByte();
    byte[] bytes = byteData.readBytes();
    return new DataFrame(id, bytes);
  }

  private AbstractByteData readDataFromChanel(SocketChannel socketChannel){
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




}
