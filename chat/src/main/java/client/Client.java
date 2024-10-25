package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.sound.sampled.*;

public class Client {
    private static final String AUDIO_FORMAT = "audio.wav";
    private Socket socket;
    private PrintWriter out;

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {
        String host = "192.168.26.130"; // Cambia esto por la IP de tu servidor
        int port = 12345;
        Scanner scanner = new Scanner(System.in);

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);

            // Iniciar el hilo para recibir mensajes y archivos
            new Thread(new ReadMessages(socket)).start();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Conectado al servidor en " + host + ":" + port);

            // Leer el mensaje del servidor para ingresar el nombre de usuario
            String serverPrompt = in.readLine();
            System.out.println("Respuesta del servidor: " + serverPrompt);

            // Enviar el nombre de usuario al servidor
            String username = scanner.nextLine();
            out.println(username);

            // Esperar la bienvenida del servidor
            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            // Bucle principal para enviar mensajes al servidor
            while (true) {
                System.out.println("\nEscribe tu mensaje:");
                System.out.println("'/privado username mensaje' para mensaje privado");
                System.out.println("'/audio username' para enviar una nota de voz");
                System.out.println("'salir' para desconectarse\n");

                String message = scanner.nextLine();

                if (message.startsWith("/audio ")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {
                        String target = parts[1];
                        File audioFile = grabarAudio();
                        if (audioFile != null) {
                            out.println("/audio " + target);
                            enviarArchivo(audioFile); // Enviar el archivo al servidor
                            System.out.println("Nota de voz enviada a " + target + ".");
                        } else {
                            System.out.println("Error al grabar el audio.");
                        }
                    } else {
                        System.out.println("Uso correcto: /audio username");
                    }
                } else if (message.equalsIgnoreCase("salir")) {
                    System.out.println("Desconectando del servidor...");
                    out.println("salir");
                    break;
                } else {
                    out.println(message);
                }
            }

            socket.close();
            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para grabar el audio y guardarlo en un archivo .wav
    private File grabarAudio() {
        File audioFile = new File(AUDIO_FORMAT);
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();
            System.out.println("Grabando audio... Presiona Enter para detener.");

            // Iniciar grabación en un hilo para poder detenerla al presionar Enter
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            Thread recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (!Thread.currentThread().isInterrupted()) {
                    int bytesRead = line.read(buffer, 0, buffer.length);
                    outStream.write(buffer, 0, bytesRead);
                }
            });
            recordingThread.start();

            // Esperar a que el usuario presione Enter para detener
            new Scanner(System.in).nextLine();
            recordingThread.interrupt();
            line.stop();
            line.close();
            System.out.println("Grabación detenida.");

            // Escribir el audio al archivo
            byte[] audioData = outStream.toByteArray();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                 AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize())) {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, audioFile);
            }

            return audioFile;

        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Método para enviar el archivo de audio al servidor
    private void enviarArchivo(File audioFile) {
        try {
            // Enviar el archivo a través del mismo socket
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(audioFile);

            // Enviar el tamaño del archivo primero
            long fileSize = audioFile.length();
            dos.writeLong(fileSize);

            // Enviar el contenido del archivo
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
            fis.close();
            System.out.println("Archivo de audio enviado correctamente.");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error al enviar el archivo de audio.");
        }
    }
}
