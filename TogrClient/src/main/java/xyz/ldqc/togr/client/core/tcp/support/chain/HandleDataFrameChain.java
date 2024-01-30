package xyz.ldqc.togr.client.core.tcp.support.chain;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import xyz.ldqc.tightcall.chain.Chain;
import xyz.ldqc.tightcall.chain.InboundChain;
import xyz.ldqc.tightcall.server.handler.ChannelHandler;
import xyz.ldqc.togr.client.core.entity.DataFrame;
import xyz.ldqc.togr.client.core.entity.SendFrame;
import xyz.ldqc.togr.client.core.tcp.support.run.StreamExchangeRunnable;
import xyz.ldqc.togr.client.exception.HandleDataFrameException;

/**
 * @author Fetters
 */
public class HandleDataFrameChain implements ChannelHandler, InboundChain {

  private static final String LOCAL_HOST = "127.0.0.1";

  private final int port;

  private Chain nextChain;

  private final Map<Long, Socket> socketMap = new HashMap<>();

  private final ExecutorService socketStreamPool;

  public HandleDataFrameChain(int port) {
    this.port = port;
    socketStreamPool = new ThreadPoolExecutor(
        0, Integer.MAX_VALUE, Integer.MAX_VALUE, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(2048), r -> new Thread(r, "stream-exchange")
    );
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
    if (!DataFrame.class.isAssignableFrom(o.getClass())) {
      return;
    }
    DataFrame dataFrame = (DataFrame) o;
    long id = dataFrame.getId();
    byte[] data = dataFrame.getData();
    Socket socket = socketMap.get(id);
    socket = checkTargetAvailable(socket, channel);
    this.socketMap.put(id, socket);
    SendFrame sendFrame = new SendFrame(socket, data);
    nextChain.doChain(channel, sendFrame);
  }


  private Socket checkTargetAvailable(Socket socket, Channel channel) {
    if (socket == null || checkTargetIsShutdown(socket)) {
      try {
        Socket newSocket = new Socket(LOCAL_HOST, this.port);
        this.socketStreamPool.execute(
            new StreamExchangeRunnable(newSocket, ((SocketChannel) channel))
        );
        return newSocket;
      } catch (IOException e) {
        throw new HandleDataFrameException("Connect fail, " + e.getMessage());
      }
    } else {
      return socket;
    }

  }

  private boolean checkTargetIsShutdown(Socket socket) {
    return !socket.isConnected() || socket.isInputShutdown() || socket.isOutputShutdown();
  }
}
