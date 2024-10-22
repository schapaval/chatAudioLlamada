package audio;

import javax.sound.sampled.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PlayerThread extends Thread {
    BlockingQueue<byte[]> buffer;
    private SourceDataLine sourceDataLine;

    public PlayerThread(AudioFormat audioFormat, int BUFFER_SIZE) {
        try {
            buffer = new LinkedBlockingQueue<>();
            sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBytes(byte[] bytes) {
        try {
            buffer.put(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                byte[] bytes = buffer.take();
                sourceDataLine.write(bytes, 0, bytes.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
