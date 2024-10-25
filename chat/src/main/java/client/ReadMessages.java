// Archivo: ReadMessages.java
package client;

import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

public class ReadMessages implements Runnable {
    private BufferedReader in;

    public ReadMessages(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                System.out.println("\nMensaje recibido: " + message);
                if (message.startsWith("Has recibido una nota")) {
                    AudioPlayer player = new AudioPlayer();
                    player.playAudio("C:\\Users\\User\\Desktop\\chatAudioLlamada\\chat\\audio.wav");
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo mensajes del servidor.");
        }
    }

    // Clase interna para reproducir audio
    private class AudioPlayer {
        public void playAudio(String filePath) {
            try {
                File audioFile = new File(filePath);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip audioClip = AudioSystem.getClip();
                audioClip.open(audioStream);
                audioClip.start();
                System.out.println("Reproduciendo audio...");
            } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
                System.out.println("Error al reproducir el audio: " + e.getMessage());
            }
        }
    }
}
