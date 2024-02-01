package xyz.ldqc.togr.server.core.tcp.support.tunnel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.togr.server.exception.ServerTunnelException;

/**
 * @author Fetters
 */
public class ServerTunnel implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ServerTunnel.class);

  private final ServerSocket serverSocket;

  private final AtomicBoolean connectedFlag = new AtomicBoolean(false);

  private Socket target;

  private final Map<Long, SocketChannel> idClientMap;

  private ExecutorService serverSocketExecutor;

  private boolean terminate = false;

  private final Object lock = new Object();

  public ServerTunnel(int port, Map<Long, SocketChannel> idClientMap) {
    this.idClientMap = idClientMap;
    try {
      this.serverSocket = new ServerSocket(port);
    } catch (IOException e) {
      throw new ServerTunnelException("Server socket init fail: " + e.getMessage());
    }
    this.serverSocketExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1), r -> new Thread(r, "server-socket"));
    serverSocketExecutor.execute(this);

  }


  @Override
  public void run() {
    synchronized (lock){
      while (!terminate){
        try {
          doRun();
          lock.wait();
          log.info("Target client disconnected, reconnecting...");
        } catch (InterruptedException e) {
          this.connectedFlag.set(false);
          refreshConnectStatus();
          log.error("Lock interrupt", e);
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  public void terminate() {
    this.terminate = true;
  }

  private void doRun(){
    try {
      this.target = this.serverSocket.accept();
      IntranetClientExecutor intranetClientExecutor = new IntranetClientExecutor(target,
          idClientMap, this);
      intranetClientExecutor.execute();
      connectedFlag.set(true);
      log.info("Target online");
    } catch (IOException e) {
      this.connectedFlag.set(false);
      refreshConnectStatus();
      throw new ServerTunnelException("Accept server fail: " + e.getMessage());
    }
  }

  public boolean isConnected() {
    return this.connectedFlag.get();
  }

  public void writeTarget(byte[] data) throws IOException {
    try {
      OutputStream outputStream = this.target.getOutputStream();
      outputStream.write(data);
      outputStream.flush();
      String dataStr = new String(data);
      log.debug("write data: {}", dataStr);
    } catch (IOException e) {
      this.connectedFlag.set(false);
      refreshConnectStatus();
      throw new IOException("Output data fail: " + e.getMessage());
    }
  }

  public void forceClose(){
    try {
      this.target.close();
      this.connectedFlag.set(false);
      refreshConnectStatus();
    } catch (IOException e) {
      log.error("Close fail, ", e);
    }
  }

  public void refreshConnectStatus() {
    this.connectedFlag.set(this.target != null && !this.target.isClosed());
    synchronized (lock){
      if (!connectedFlag.get()){
        lock.notifyAll();
        log.info("Target offline");
      }
    }
  }
}
