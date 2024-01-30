package xyz.ldqc.togr.client.core.entity;

import java.net.Socket;

/**
 * @author Fetters
 */
public class SendFrame {

  private Socket socket;

  private byte[] data;

  public SendFrame(Socket socket, byte[] data) {
    this.socket = socket;
    this.data = data;
  }

  public Socket getSocket() {
    return socket;
  }

  public void setSocket(Socket socket) {
    this.socket = socket;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }
}
