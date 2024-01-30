package xyz.ldqc.togr.client.core.tcp;

import java.net.InetSocketAddress;
import xyz.ldqc.tightcall.chain.ChainGroup;
import xyz.ldqc.tightcall.chain.support.DefaultChannelChainGroup;
import xyz.ldqc.tightcall.client.exce.support.NioClientExec;
import xyz.ldqc.togr.client.core.tcp.support.chain.ExchangeHandlerChain;
import xyz.ldqc.togr.client.core.tcp.support.chain.HandleDataFrameChain;
import xyz.ldqc.togr.client.core.tcp.support.chain.PostHandleChain;

/**
 * @author Fetters
 */
public class TcpTunnelClient {

  private final int targetPort;

  public TcpTunnelClient(String ip, int port, int targetPort) {
    this.targetPort = targetPort;
    NioClientExec nioClientExec = new NioClientExec(new InetSocketAddress(ip, port));
    nioClientExec.setChainGroup(buildChainGroup());
    nioClientExec.start();
  }

  private ChainGroup buildChainGroup(){
    ChainGroup group = new DefaultChannelChainGroup();
    group.addLast(new ExchangeHandlerChain(targetPort));
    group.addLast(new HandleDataFrameChain(targetPort));
    group.addLast(new PostHandleChain());
    return group;
  }

}
