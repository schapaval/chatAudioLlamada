// AudioManager.java
package audio;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class AudioManager {
    private static final int BUFFER_SIZE = 2048; // Increased buffer size
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        22050.0f, // Lower sample rate (22.05 kHz instead of 44.1 kHz)
        16,       // 16 bits per sample
        1,        // Mono channel instead of stereo
        true,     // Signed
        false     // Little endian
    );

    private DatagramSocket audioSocket;
    private volatile boolean isRecording = false;
    private volatile boolean isPlaying = false;
    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
    private int port;

    public AudioManager(int port) throws SocketException {
        try {
            this.port = port;
            this.audioSocket = new DatagramSocket(port);
            // Pre-initialize audio lines
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);

            micLine = (TargetDataLine) AudioSystem.getLine(micInfo);
            speakerLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            // Check if the lines are supported before attempting to open them
            if (!AudioSystem.isLineSupported(micInfo)) {
                throw new RuntimeException("Microphone line is not supported.");
            }
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                throw new RuntimeException("Speaker line is not supported.");
}


            // Open lines in advance
            micLine.open(AUDIO_FORMAT);
            speakerLine.open(AUDIO_FORMAT);
        } catch (LineUnavailableException e) {
            throw new RuntimeException("Failed to initialize audio system", e);
        }
    }

    public void startRecording(InetAddress targetAddress, int targetPort) {
        if (isRecording) return;

        isRecording = true;
        Thread recordThread = new Thread(() -> {
            try {
                micLine.start();
                byte[] buffer = new byte[BUFFER_SIZE];

                while (isRecording) {
                    int count = micLine.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer, count, targetAddress, targetPort);
                        audioSocket.send(packet);
                    }
                }

                micLine.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        recordThread.setName("AudioRecordThread");
        recordThread.start();
    }

    public void startPlaying() {
        if (isPlaying) return;

        isPlaying = true;
        Thread playThread = new Thread(() -> {
            try {
                speakerLine.start();
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (isPlaying) {
                    audioSocket.receive(packet);
                    speakerLine.write(packet.getData(), 0, packet.getLength());
                }

                speakerLine.drain();
                speakerLine.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        playThread.setName("AudioPlayThread");
        playThread.start();
    }

    public void stopRecording() {
        isRecording = false;
        if (micLine != null) {
            micLine.stop();
        }
    }

    public void stopPlaying() {
        isPlaying = false;
        if (speakerLine != null) {
            speakerLine.stop();
        }
    }

    public void close() {
        stopRecording();
        stopPlaying();
        if (micLine != null && micLine.isOpen()) {
            micLine.close();
        }
        if (speakerLine != null && speakerLine.isOpen()) {
            speakerLine.close();
        }
        if (audioSocket != null && !audioSocket.isClosed()) {
            audioSocket.close();
        }
    }

    public int getPort() {
        return this.port;
    }
}
