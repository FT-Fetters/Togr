package xyz.ldqc.togr.client.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.togr.client.core.tcp.TcpTunnelClient;

/**
 * @author Fetters
 */
public class TogrClientMainBoot {

  private static final Logger log = LoggerFactory.getLogger(TogrClientMainBoot.class);

  private static final int ARG_LEN = 4;

  public static void main(String[] args) {
    if (args.length < ARG_LEN){
      log.info("Miss arg");
      return;
    }
    try {
      String serverAddress = args[0];
      int serverPort = Integer.parseInt(args[1]);
      String targetAddress = args[2];
      int targetPort = Integer.parseInt(args[3]);
      new TcpTunnelClient(serverAddress, serverPort, targetAddress, targetPort);
    }catch (Exception e){
      log.error("Boot fail: {}", e.getMessage());
    }
  }

}
