import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import xyz.ldqc.tightcall.util.DigestUtil;
import xyz.ldqc.togr.server.core.tcp.TcpTunnelServer;

public class CommonTest {

  @Test
  public void testServer(){
    TcpTunnelServer tcpTunnelServer = new TcpTunnelServer(7777);
    LockSupport.park();
  }

  @Test
  public void byte2longTest(){
    long l = 1201923333549981696L;
    byte[] bytes = DigestUtil.long2byte(l);
    long ll = DigestUtil.byte2long(bytes);
    System.out.println(ll);

  }

}
