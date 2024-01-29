import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import xyz.ldqc.togr.server.core.tcp.TcpTunnelServer;

public class CommonTest {

  @Test
  public void testServer(){
    TcpTunnelServer tcpTunnelServer = new TcpTunnelServer(7777);
    LockSupport.park();
  }

}
