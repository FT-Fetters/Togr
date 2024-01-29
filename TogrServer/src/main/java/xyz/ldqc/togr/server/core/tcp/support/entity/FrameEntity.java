package xyz.ldqc.togr.server.core.tcp.support.entity;

/**
 * @author Fetters
 */
public class FrameEntity {

  private long id;

  private byte[] data;

  public FrameEntity(long id, byte[] data) {
    this.id = id;
    this.data = data;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }
}
