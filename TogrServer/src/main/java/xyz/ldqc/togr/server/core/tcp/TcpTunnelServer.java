package xyz.ldqc.togr.server.core.tcp;

import xyz.ldqc.tightcall.chain.ChainGroup;
import xyz.ldqc.tightcall.chain.support.DefaultChannelChainGroup;
import xyz.ldqc.tightcall.server.exec.support.NioServerExec;
import xyz.ldqc.togr.server.core.TunnelServer;
import xyz.ldqc.togr.server.core.tcp.support.chain.DataInChain;

/**
 * @author Fetters
 */
public class TcpTunnelServer implements TunnelServer {

  public TcpTunnelServer(int port){
    NioServerExec nioServerExec = new NioServerExec(port, 2);
    nioServerExec.setChainGroup(buildChainGroup());
    nioServerExec.start();

  }

  private ChainGroup buildChainGroup(){
    ChainGroup chainGroup = new DefaultChannelChainGroup();
    chainGroup.addLast(new DataInChain(8888));
    return chainGroup;
  }

  @Override
  public void boot() {

  }
}
