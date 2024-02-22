package xyz.ldqc.togr.client.core.tcp.support.run;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ldqc.tightcall.buffer.SimpleByteData;

/**
 * @author Fetters
 */
public class StreamExchangeRunnable implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(StreamExchangeRunnable.class);

    private static final String CLOSE_FLAG_MSG = "Socket closed";

    private static final byte[] MAGIC_NUM = "TOGR".getBytes(StandardCharsets.UTF_8);

    private static final byte[] CLOSE_FLAG = "cls".getBytes(StandardCharsets.UTF_8);


    /**
     * 连接目标服务器的客户端
     */
    private final Socket socket;

    /**
     * 公网服务器的通道
     */
    private final SocketChannel channel;

    private final ByteBuffer byteBuffer;

    private final int id;

    private final CountDownLatch connectLock;

    public StreamExchangeRunnable(Socket socket, int id, SocketChannel channel,
        CountDownLatch connectLock) {
        this.socket = socket;
        this.channel = channel;
        this.id = id;
        this.connectLock = connectLock;
        this.byteBuffer = ByteBuffer.allocate(4096 + 8);
    }

    @Override
    public void run() {
        try {
            connectLock.await();
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
            Thread.currentThread().interrupt();
        }
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            InputStream inputStream = this.socket.getInputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 从目标服务器返回的数据
                byteBuffer.clear();
                byteBuffer.put(MAGIC_NUM);
                byteBuffer.put(int2TwoByte(id));
                byteBuffer.put(int2TwoByte(bytesRead));
                byteBuffer.put(buffer, 0, bytesRead);
                byteBuffer.flip();
                writeToChannel(byteBuffer);
            }
            log.info("Client {} closed", socket.getInetAddress());
            sendClsFlag();
        } catch (IOException e) {
            if (e.getMessage().equals(CLOSE_FLAG_MSG)) {
                log.info("Server close connection");
            } else {
                log.error("Client {} closed with exception", socket.getInetAddress(), e);
            }
        }
    }

    private void sendClsFlag() {
        SimpleByteData byteData = new SimpleByteData();
        byteData.writeBytes(MAGIC_NUM).writeBytes(int2TwoByte(id))
            .writeBytes(int2TwoByte(CLOSE_FLAG.length))
            .writeBytes("cls".getBytes(StandardCharsets.UTF_8));
        ByteBuffer clsBuffer = ByteBuffer.allocate(byteData.remaining());
        clsBuffer.clear();
        clsBuffer.put(byteData.readBytes());
        clsBuffer.flip();
        try {
            channel.write(clsBuffer);
        } catch (IOException ex) {
            log.error("Send close flag fail", ex);
        }
    }

    private void writeToChannel(ByteBuffer buffer) throws IOException {
        int remaining = buffer.remaining();
        while (remaining > 0) {
            int write = channel.write(buffer);
            remaining -= write;
        }
    }

    private byte[] int2TwoByte(int i) {
        byte[] bytes = new byte[2];
        int b0 = (i >> 8) - 128;
        int b1 = (i & 0xFF) - 128;
        bytes[0] = ((byte) b0);
        bytes[1] = ((byte) b1);
        return bytes;
    }
}
