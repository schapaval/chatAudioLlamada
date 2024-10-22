package audio;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class AudioStreamManager {
    private static final AudioFormat FORMAT = new AudioFormat(44100.0f, 16, 1, true, true);
    private static final int BUFFER_SIZE = 4096;
    
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private DatagramSocket socket;
    private boolean isStreaming;
    private Thread recordingThread;
    private Thread playbackThread;

    public AudioStreamManager(int port) throws LineUnavailableException, SocketException {
        // Configurar micrófono
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, FORMAT);
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
        microphone.open(FORMAT);

        // Configurar altavoces
        DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, FORMAT);
        speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
        speakers.open(FORMAT);

        socket = new DatagramSocket(port);
    }

    public void startStreaming(InetAddress targetAddress, int targetPort) {
        if (isStreaming) return;
        isStreaming = true;

        // Iniciar grabación
        recordingThread = new Thread(() -> {
            microphone.start();
            byte[] buffer = new byte[BUFFER_SIZE];
            
            while (isStreaming) {
                int count = microphone.read(buffer, 0, buffer.length);
                if (count > 0) {
                    try {
                        DatagramPacket packet = new DatagramPacket(
                            buffer, count, targetAddress, targetPort);
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            microphone.stop();
        });

        // Iniciar reproducción
        playbackThread = new Thread(() -> {
            speakers.start();
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (isStreaming) {
                try {
                    socket.receive(packet);
                    speakers.write(packet.getData(), 0, packet.getLength());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            speakers.stop();
        });

        recordingThread.start();
        playbackThread.start();
    }

    public void stopStreaming() {
        isStreaming = false;
        if (recordingThread != null) {
            recordingThread.interrupt();
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
        }
        microphone.close();
        speakers.close();
    }
}
