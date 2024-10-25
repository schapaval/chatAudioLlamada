// Archivo: Client.java
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
        String host = "192.168.26.130";
        int port = 12345;
        Scanner scanner = new Scanner(System.in);
        new Thread(new AudioReceptor(socket)).start();
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Conectado al servidor en " + host + ":" + port);

            // Leer el mensaje del servidor para ingresar el username
            String serverPrompt = in.readLine();
            System.out.println("Respuesta del servidor: " + serverPrompt);

            // Enviar el username al servidor
            String username = scanner.nextLine();
            out.println(username);

            // Esperar la bienvenida del servidor
            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            // Iniciar el hilo para leer mensajes del servidor
            new Thread(new ReadMessages(in)).start();

            // Hilo principal para enviar mensajes al servidor
            while (true) {
                System.out.println("\nEscribe tu mensaje:");
                System.out.println("'/privado username mensaje' para mensaje privado");
                System.out.println("'/grupo groupname mensaje' para mensaje de grupo");
                System.out.println("'/creargrupo groupname user1,user2,...' para crear un grupo");
                System.out.println("'/audio username|groupname' para enviar una nota de voz");
                System.out.println("'/llamada username|groupname' para realizar una llamada");
                System.out.println("'salir' para desconectarse\n");

                String message = scanner.nextLine();

                if (message.startsWith("/audio ")) {
                    String target = message.split(" ", 2)[1];
                    File audioFile = grabarAudio();
                    if (audioFile != null) {
                        out.println("/audio " + target);
                        Socket client2 = enviarArchivo(audioFile, socket);
                        System.out.println("Nota de voz enviada a " + target + ".");
                    } else {
                        System.out.println("Error al grabar el audio.");
                    }
                } else if (message.startsWith("/llamada ")) {
                    out.println(message);
                    // Aquí puedes implementar la lógica para manejar llamadas
                } else {
                    out.println(message);
                }

                if (message.equalsIgnoreCase("salir")) {
                    System.out.println("Desconectando del servidor...");
                    break;
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
        AudioFormat format = new AudioFormat(16000, 16, 2, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();
            System.out.println("Grabando audio... Presiona Enter para detener.");

            // Iniciar grabación en un hilo para que podamos detenerla al presionar Enter
            try (AudioInputStream audioStream = new AudioInputStream(line)) {
                new Thread(() -> {
                    try {
                        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                // Espera a que el usuario presione Enter para detener
                new Scanner(System.in).nextLine();
                line.stop();
                line.close();
                System.out.println("Grabación detenida.");
                return audioFile;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Método para enviar el archivo de audio a través de un nuevo socket
    private Socket enviarArchivo(File audioFile, Socket socket) {
        try {
            // Crear un nuevo socket en el mismo host y puerto para enviar el archivo
            Socket fileSocket = new Socket(socket.getInetAddress(), socket.getPort());

            // Crear flujo de salida para enviar el archivo
            try (BufferedOutputStream bos = new BufferedOutputStream(fileSocket.getOutputStream());
                 FileInputStream fis = new FileInputStream(audioFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                }
                bos.flush();
                System.out.println("Archivo de audio enviado correctamente.");
            }

            return fileSocket;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error al enviar el archivo de audio.");
        }
        return null;
    }


}
