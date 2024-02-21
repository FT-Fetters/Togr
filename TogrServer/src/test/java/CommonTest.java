import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import xyz.ldqc.tightcall.util.DigestUtil;
import xyz.ldqc.togr.server.core.tcp.TcpTunnelServer;

public class CommonTest {

  @Test
  public void testServer() {
    TcpTunnelServer tcpTunnelServer = new TcpTunnelServer(7777);
    LockSupport.park();
  }

  @Test
  public void byte2longTest() {
    long l = 1201923333549981696L;
    byte[] bytes = DigestUtil.long2byte(l);
    long ll = DigestUtil.byte2long(bytes);
    System.out.println(ll);

  }

  @Test
  public void twoByte2Uint() {
    byte[] bytes = {-128, 0};
    int b1 = bytes[0] + 128;
    int b2 = bytes[1] + 128;
    int i = ((b1 & 0xff) << 8) | (b2 & 0xff);
    System.out.println(i);

  }

  @Test
  public void int2TwoByte() {
    int i = 128;
    byte[] bytes = new byte[2];
    int b0 = (i >> 8) - 128;
    int b1 = (i & 0xFF) - 128;
    bytes[0] = ((byte) b0);
    bytes[1] = ((byte) b1);

    System.out.println(b0);
    System.out.println(b1);
  }

}
