import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import xyz.ldqc.togr.client.core.tcp.TcpTunnelClient;

public class ClientTest {

  @Test
  public void testClient(){
    new TcpTunnelClient("127.0.0.1", 7086, "10.1.3.37", 8011);
    LockSupport.park();
  }

}
