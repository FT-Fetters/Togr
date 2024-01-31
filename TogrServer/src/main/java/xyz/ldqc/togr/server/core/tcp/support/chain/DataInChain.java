package xyz.ldqc.togr.server.core.tcp.support.chain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.tightcall.util.SnowflakeUtil;
import xyz.ldqc.togr.server.exception.ServerTunnelException;

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
    this.serverTunnel = new ServerTunnel(port, clientIdMap, idClientMap);
  }

  @Override
  public void doChain(Channel channel, Object o) {
    if (!SocketChannel.class.isAssignableFrom(channel.getClass())) {
      return;
    }
    if (!(o instanceof SelectionKey)) {
      return;
    }
    if (!serverTunnel.isConnected()){
      return;
    }
    SocketChannel socketChannel = (SocketChannel) channel;
    SelectionKey selectionKey = (SelectionKey) o;

    clientIdMap.computeIfAbsent(socketChannel, s -> snow.nextId());
    Long clientId = clientIdMap.get(socketChannel);
    idClientMap.putIfAbsent(clientId, socketChannel);
    SimpleByteData byteData = readDataFromChanel(socketChannel);
    if (byteData == null){
      selectionKey.cancel();
      SimpleByteData bd = new SimpleByteData();
      bd.writeBytes("cls".getBytes(StandardCharsets.UTF_8));
      byte[] bytes = buildSendBytes(bd, clientId);
      this.serverTunnel.writeTarget(bytes);
      return;
    }
    log.debug("{} send: {}", socketChannel, byteData);


    byte[] frameData = buildSendBytes(byteData, clientId);

    this.serverTunnel.writeTarget(frameData);

  }


  private SimpleByteData readDataFromChanel(SocketChannel socketChannel){
    ByteBuffer buffer = ByteBuffer.allocate(127);
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

  private byte[] buildSendBytes(SimpleByteData byteData, long id){
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

  }

  private static class ServerTunnel implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ServerTunnel.class);

    private final ServerSocket serverSocket;

    private final AtomicBoolean connectedFlag = new AtomicBoolean(false);

    private Socket target;

    private final Map<SocketChannel, Long> clientIdMap;

    private final Map<Long, SocketChannel> idClientMap;

    public ServerTunnel(int port, Map<SocketChannel, Long> clientIdMap, Map<Long, SocketChannel> idClientMap) {
      this.clientIdMap = clientIdMap;
      this.idClientMap = idClientMap;
      try {
        this.serverSocket = new ServerSocket(port);
      } catch (IOException e) {
        throw new ServerTunnelException("Server socket init fail: " + e.getMessage());
      }
      ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(1), r -> new Thread(r, "server-socket"));
      executorService.execute(this);

    }


    @Override
    public void run() {
      try {
        this.target = this.serverSocket.accept();
        new Thread(() -> {
          try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            InputStream inputStream = this.target.getInputStream();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
              System.out.println("Read " + bytesRead + " bytes");
              SimpleByteData byteData = new SimpleByteData();
              for (int i = 0; i < 9; i++) {
                byteData.writeByte(buffer[i]);
              }
              long id = byteData.readLong();
              SocketChannel socketChannel = idClientMap.get(id);

              ByteBuffer buf = ByteBuffer.allocate(bytesRead);
              if (bytesRead == 8 + "cls".getBytes().length){
                byte[] bs = {buffer[8], buffer[9], buffer[10]};
                if ("cls".equals(new String(bs))){
                  socketChannel.close();
                  return;
                }
              }

              buf.clear();
              buf.put(buffer,9, bytesRead - 9);
              buf.flip();
              byte[] array = buf.array();
              log.info("Recieve: {}", new String(array));
              socketChannel.write(buf);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }).start();
        connectedFlag.set(true);
        log.info("Target online");
      } catch (IOException e) {
        this.connectedFlag.set(false);
        throw new ServerTunnelException("Accept server fail: " + e.getMessage());
      }
    }

    public boolean isConnected(){
      return this.connectedFlag.get();
    }

    public void writeTarget(byte[] data){
      try {
        OutputStream outputStream = this.target.getOutputStream();
        outputStream.write(data);
        outputStream.flush();
        log.debug("write data: {}", new String(data));
      } catch (IOException e) {
        throw new ServerTunnelException("Out put data fail: "+ e.getMessage());
      }
    }
  }
}
