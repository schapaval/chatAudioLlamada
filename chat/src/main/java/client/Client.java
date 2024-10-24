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
        String host = "192.168.119.130";
        int port = 12345;
        Scanner scanner = new Scanner(System.in);

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

    // Método para grabar audio
    private static File grabarAudio() {
        File audioFile = new File("audio.wav");
        AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info)) {
            microphone.open(format);
            AudioInputStream audioInputStream = new AudioInputStream(microphone);

            System.out.println("Grabando... Presiona ENTER para detener la grabación.");
            microphone.start();

            Thread stopper = new Thread(() -> {
                new Scanner(System.in).nextLine(); // Detener la grabación cuando se presiona ENTER
                microphone.stop();
                microphone.close();
            });
            stopper.start();

            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, audioFile);
            System.out.println("Grabación finalizada.");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return audioFile;
    }


    // Método para enviar un archivo al servidor
    private Socket enviarArchivo(File file, Socket socket) {
        try {
            byte[] buffer = new byte[4096]; // Tamaño del buffer
            OutputStream os = socket.getOutputStream();
            FileInputStream fis = new FileInputStream(file);

            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                return this.socket;
            }
            os.flush();
            fis.close();
            System.out.println("Archivo enviado con éxito.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
