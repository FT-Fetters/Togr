package xyz.ldqc.togr.server.core.tcp.support.chain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.tightcall.util.SnowflakeUtil;
import xyz.ldqc.togr.server.core.tcp.support.tunnel.ServerTunnel;

/**
 * @author Fetters
 */
public class DataInChain implements InboundChain, ChannelHandler {

  private static final Logger log = LoggerFactory.getLogger(DataInChain.class);

  private Chain nextChain;

  private final ServerTunnel serverTunnel;

  private final Map<SocketChannel, Long> clientIdMap;

  private final Map<Long, SocketChannel> idClientMap;

  private final SnowflakeUtil snow = SnowflakeUtil.getInstance();


  public DataInChain(int port) {
    clientIdMap = new ConcurrentHashMap<>();
    idClientMap = new ConcurrentHashMap<>();
    this.serverTunnel = new ServerTunnel(port, idClientMap);
  }

  @Override
  public void doChain(Channel channel, Object o) {
    if (!SocketChannel.class.isAssignableFrom(channel.getClass())) {
      return;
    }
    if (!(o instanceof SelectionKey)) {
      return;
    }
    if (!serverTunnel.isConnected()) {
      closeChannel(channel);
      return;
    }
    doHandler(channel, o);
  }


  private SimpleByteData readDataFromChanel(SocketChannel socketChannel) {
    ByteBuffer buffer = ByteBuffer.allocate(127);
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
      return null;
    }
  }

  private byte[] buildSendBytes(SimpleByteData byteData, long id) {
    int dataLen = byteData.remaining();
    SimpleByteData resByte = new SimpleByteData(8 + 1 + dataLen);
    resByte.writeLong(id);
    resByte.writeByte((byte) dataLen);
    resByte.writeBytes(byteData.readBytes());
    return resByte.readBytes();
  }

  @Override
  public void setNextChain(Chain chain) {
    this.nextChain = chain;
  }

  @Override
  public void doHandler(Channel channel, Object o) {
    SocketChannel socketChannel = (SocketChannel) channel;
    SelectionKey selectionKey = (SelectionKey) o;



    clientIdMap.computeIfAbsent(socketChannel, s -> snow.nextId());
    Long clientId = clientIdMap.get(socketChannel);
    idClientMap.putIfAbsent(clientId, socketChannel);
    SimpleByteData byteData = readDataFromChanel(socketChannel);
    if (byteData == null) {
      // 如果为null代表已经断开连接，则直接向内网发起断开标识
      selectionKey.cancel();
      SimpleByteData bd = new SimpleByteData();
      bd.writeBytes("cls".getBytes(StandardCharsets.UTF_8));
      byte[] bytes = buildSendBytes(bd, clientId);
      try {
        this.serverTunnel.writeTarget(bytes);
      } catch (IOException e) {
        log.error("Write fail", e);
      }
      return;
    }
    log.debug("{} send: {}", socketChannel, byteData);

    byte[] frameData = buildSendBytes(byteData, clientId);

    try {
      this.serverTunnel.writeTarget(frameData);
    } catch (IOException e) {
      closeChannel(channel);
      log.error("Write fail", e);
    }
  }

  private void closeChannel(Channel channel){
    try {
      channel.close();
    } catch (IOException e) {
      log.error("Close fail, {}", e.getMessage(), e);
    }
  }


}
