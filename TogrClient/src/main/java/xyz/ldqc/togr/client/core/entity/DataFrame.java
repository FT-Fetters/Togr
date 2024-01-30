package xyz.ldqc.togr.client.core.entity;

/**
 * @author Fetters
 */
public class DataFrame {

  private long id;

  private byte[] data;

  public DataFrame(long id, byte[] data) {
    this.id = id;
    this.data = data;
  }

  public DataFrame() {
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
