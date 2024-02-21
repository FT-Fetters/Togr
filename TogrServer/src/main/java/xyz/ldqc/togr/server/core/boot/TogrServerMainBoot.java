package xyz.ldqc.togr.server.core.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.togr.server.core.tcp.TcpTunnelServer;

/**
 * @author Fetters
 */
public class TogrServerMainBoot {

  private static final Logger log = LoggerFactory.getLogger(TogrServerMainBoot.class);

  private static final int ARG_LEN = 1;

  public static void main(String[] args) {
    if (args.length < ARG_LEN){
      log.info("Miss arg");
      return;
    }
    try {
      int serverPort = Integer.parseInt(args[0]);
      new TcpTunnelServer(serverPort).boot();
    }catch (Exception e){
      log.error("Boot fail: {}", e.getMessage());
    }
  }

}
