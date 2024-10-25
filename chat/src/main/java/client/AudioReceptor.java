package client;
import java.io.*;
import java.net.Socket;

import javax.sound.sampled.*;

public class AudioReceptor implements Runnable{
    private Socket socket;

    public AudioReceptor(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[4096];
            File audioFile = new File("received_audio.wav");

            try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Audio recibido. Reproduciendo...");
            playAudio(audioFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para reproducir el archivo de audio
    private void playAudio(File audioFile) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
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
