package client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;

public class ReadMessages implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private DataInputStream dataIn;

    public ReadMessages(Socket socket) {
        this.socket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.dataIn = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                System.out.println("\nMensaje recibido: " + message);
                if (message.startsWith("Has recibido una nota de voz de ")) {
                    // Recibir el archivo de audio
                    File audioFile = recibirArchivo();
                    if (audioFile != null) {
                        // Reproducir el audio
                        playAudio(audioFile);
                    } else {
                        System.out.println("Error al recibir el archivo de audio.");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo mensajes del servidor.");
        }
    }

    // Método para recibir el archivo de audio
    private File recibirArchivo() {
        try {
            // Leer el tamaño del archivo primero
            long fileSize = dataIn.readLong();

            // Crear un archivo para almacenar el audio recibido
            File audioFile = new File("received_audio_" + System.currentTimeMillis() + ".wav");
            FileOutputStream fos = new FileOutputStream(audioFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while (totalBytesRead < fileSize && (bytesRead = dataIn.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            fos.close();

            if (totalBytesRead == fileSize) {
                System.out.println("Archivo de audio recibido correctamente.");
                return audioFile;
            } else {
                System.out.println("Error al recibir el archivo de audio.");
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
            System.out.println("Reproduciendo audio...");
            while ((bytesRead = audioInputStream.read(bytesBuffer)) != -1) {
                audioLine.write(bytesBuffer, 0, bytesRead);
            }

            audioLine.drain();
            audioLine.close();
            System.out.println("Reproducción de audio finalizada.");
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            System.out.println("Error al reproducir el audio: " + ex.getMessage());
        }
    }
}
