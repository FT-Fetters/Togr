package xyz.ldqc.togr.client.core.entity;

/**
 * @author Fetters
 */
public class DataFrame {

    private int id;

    private byte[] data;

    public DataFrame(int id, byte[] data) {
        this.id = id;
        this.data = data;
    }

    public DataFrame() {
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
