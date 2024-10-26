package audio;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class AudioManager {
    private static final int BUFFER_SIZE = 1024;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000, 16, 1, true, false);
    
    private DatagramSocket audioSocket;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    
    public AudioManager(int port) throws SocketException {
        this.audioSocket = new DatagramSocket(port);
    }
    
    public void startRecording(InetAddress targetAddress, int targetPort) {
        if (isRecording) return;
        
        isRecording = true;
        Thread recordThread = new Thread(() -> {
            try {
                TargetDataLine line = AudioSystem.getTargetDataLine(AUDIO_FORMAT);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(AUDIO_FORMAT);
                line.start();

                byte[] buffer = new byte[BUFFER_SIZE];
                while (isRecording) {
                    int count = line.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer, count, targetAddress, targetPort);
                        audioSocket.send(packet);
                    }
                }
                
                line.stop();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        recordThread.start();
    }
    
    public void stopRecording() {
        isRecording = false;
    }
    
    public void startPlaying() {
        if (isPlaying) return;
        
        isPlaying = true;
        Thread playThread = new Thread(() -> {
            try {
                SourceDataLine line = AudioSystem.getSourceDataLine(AUDIO_FORMAT);
                line.open(AUDIO_FORMAT);
                line.start();
                
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                while (isPlaying) {
                    audioSocket.receive(packet);
                    line.write(packet.getData(), 0, packet.getLength());
                }
                
                line.drain();
                line.stop();
                line.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        playThread.start();
    }
    
    public void stopPlaying() {
        isPlaying = false;
    }
    
    public void close() {
        audioSocket.close();
    }

    public void playAudio(String s) {
        try {
            File audioFile = new File(s);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();

            byte[] bytesBuffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = audioInputStream.read(bytesBuffer)) != -1) {
                audioLine.write(bytesBuffer, 0, bytesRead);
            }

            audioLine.drain();
            audioLine.close();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }
}