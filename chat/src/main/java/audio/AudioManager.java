package audio;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class AudioManager {
    private static final int BUFFER_SIZE = 4096;
    private static final AudioFormat AUDIO_FORMAT = 
        new AudioFormat(16000, 16, 1, true, false);

    private DatagramSocket audioSocket;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    // Constructor con puerto dinámico
    public AudioManager(int port) throws SocketException {
        this.audioSocket = new DatagramSocket(port);
        System.out.println("AudioManager iniciado en el puerto: " + port);
    }

    // Inicia la grabación y transmisión del audio
    public void startRecording(InetAddress targetAddress, int targetPort) {
        if (isRecording) return; // Evitar múltiples grabaciones simultáneas

        isRecording = true;
        new Thread(() -> {
            try {
                TargetDataLine microphone = AudioSystem.getTargetDataLine(AUDIO_FORMAT);
                microphone.open(AUDIO_FORMAT);
                microphone.start();

                byte[] buffer = new byte[BUFFER_SIZE];
                System.out.println("Grabando audio...");

                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        DatagramPacket packet = 
                            new DatagramPacket(buffer, bytesRead, targetAddress, targetPort);
                        audioSocket.send(packet); // Enviar audio
                    }
                }

                microphone.stop();
                microphone.close();
                System.out.println("Grabación detenida.");
            } catch (LineUnavailableException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Detener la grabación
    public void stopRecording() {
        isRecording = false;
    }

    // Inicia la reproducción del audio recibido
    public void startPlaying() {
        if (isPlaying) return; // Evitar múltiples reproducciones simultáneas

        isPlaying = true;
        new Thread(() -> {
            try {
                SourceDataLine speakers = AudioSystem.getSourceDataLine(AUDIO_FORMAT);
                speakers.open(AUDIO_FORMAT);
                speakers.start();

                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                System.out.println("Esperando audio...");

                while (isPlaying) {
                    audioSocket.receive(packet); // Recibir audio
                    speakers.write(packet.getData(), 0, packet.getLength()); // Reproducir audio
                }

                speakers.drain();
                speakers.stop();
                speakers.close();
                System.out.println("Reproducción detenida.");
            } catch (LineUnavailableException | IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Detener la reproducción
    public void stopPlaying() {
        isPlaying = false;
    }

    // Cerrar el socket de audio
    public void close() {
        audioSocket.close();
    }
}
