package xyz.ldqc.togr.server.core.entity;

/**
 * @author Fetters
 */
public class FrameEntity {

  private int id;

  private byte[] data;

  public FrameEntity(int id, byte[] data) {
    this.id = id;
    this.data = data;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }
}
