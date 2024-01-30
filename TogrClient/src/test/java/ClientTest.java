import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import xyz.ldqc.togr.client.core.tcp.TcpTunnelClient;

public class ClientTest {

  @Test
  public void testClient(){
    new TcpTunnelClient("127.0.0.1", 8888, 8771);
    LockSupport.park();
  }

}
