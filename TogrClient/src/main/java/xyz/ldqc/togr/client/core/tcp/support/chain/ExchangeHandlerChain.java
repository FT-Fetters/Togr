package xyz.ldqc.togr.client.core.tcp.support.chain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.buffer.AbstractByteData;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.togr.client.core.entity.DataFrame;
import xyz.ldqc.togr.client.core.tcp.support.cache.NetTransferCache;

/**
 * @author Fetters
 */
public class ExchangeHandlerChain implements ChannelHandler, InboundChain {

  private static final Logger log = LoggerFactory.getLogger(ExchangeHandlerChain.class);

  private final NetTransferCache netTransferCache;

  private Chain nextChain;

  public ExchangeHandlerChain() {
    netTransferCache = new NetTransferCache();
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
    if (byteData == null) {
      selectionKey.cancel();
      return;
    }
//    log.debug("Receive data: {}", byteData);
    List<DataFrame> dataFrames = readDataFrame(byteData);
    for (DataFrame dataFrame : dataFrames) {
      nextChain.doChain(channel, dataFrame);
    }
  }

  private List<DataFrame> readDataFrame(AbstractByteData byteData) {
    netTransferCache.append(byteData.readBytes());
    return netTransferCache.consume();
  }

  private AbstractByteData readDataFromChanel(SocketChannel socketChannel) {
    ByteBuffer buffer = ByteBuffer.allocate(4096 + 8);
    try {
      int readLen = socketChannel.read(buffer);
      if (readLen == -1) {
        return null;
      }
      if (readLen == 0) {
        return new SimpleByteData();
      }
      return new SimpleByteData(buffer);
    } catch (IOException e) {
      log.error("readDataFromChanel error", e);
      return null;
    }
  }


}
