package xyz.ldqc.togr.client.core.tcp.support.cache;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import xyz.ldqc.tightcall.buffer.AbstractByteData;
import xyz.ldqc.tightcall.buffer.SimpleByteData;
import xyz.ldqc.togr.client.core.entity.DataFrame;

/**
 * @author Fetters
 */
public class NetTransferCache {

  private static final int COM_LEN = 2;

  /**
   * 前缀长度，包含 TOGR 魔数4位，长度2位，id2位
   */
  private static final int PREFIX_LEN = 4 + 2 + 2;

  private static final byte[] MAGIC_NUM = "TOGR".getBytes(StandardCharsets.UTF_8);

  private boolean ready;

  private int len;

  private int id;

  private final AbstractByteData cacheData;

  private final ConcurrentLinkedDeque<DataFrame> frameQueue;

  public NetTransferCache() {
    this.ready = false;
    this.len = -1;
    cacheData = new SimpleByteData(1024);
    frameQueue = new ConcurrentLinkedDeque<>();
  }

  public void append(byte[] data) {
    if (ready) {
      doAppend(data);
    } else {
      doFirstAppend(data);
    }
  }

  private void doAppend(byte[] data){
    cacheData.writeBytes(data);
    if (cacheData.remaining() < len){
      return;
    }
    byte[] d = cacheData.readBytes(len);
    DataFrame frameEntity = new DataFrame(id, d);
    frameQueue.offer(frameEntity);
    ready = false;
    if (!cacheData.isEmpty()){
      append(new byte[0]);
    }
  }


  private void doFirstAppend(byte[] data) {
    cacheData.writeBytes(data);
    if (cacheData.remaining() < PREFIX_LEN) {
      return;
    }
    byte[] mag = cacheData.readBytes(4);
    if (!Arrays.equals(mag, MAGIC_NUM)) {
      throw new IllegalStateException("Error transfer type");
    }
    byte[] idBytes = cacheData.readBytes(2);
    this.id = twoByte2UsInt(idBytes);
    byte[] lenBytes = cacheData.readBytes(2);
    this.len = twoByte2UsInt(lenBytes);
    ready = true;
    if (!cacheData.isEmpty()) {
      append(new byte[0]);
    }
  }

  private int twoByte2UsInt(byte[] bs) {
    if (bs.length != COM_LEN) {
      return 0;
    }
    int bl = bs[0] + 128;
    int br = bs[1] + 128;
    return ((bl & 0xff) << 8) | (br & 0xff);
  }

  public List<DataFrame> consume() {
    if (frameQueue.isEmpty()) {
      return Collections.emptyList();
    }
    List<DataFrame> r = new LinkedList<>();
    while (!frameQueue.isEmpty()) {
      r.add(frameQueue.poll());
    }
    return r;
  }


}
