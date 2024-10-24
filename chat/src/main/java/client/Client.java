// Archivo: Client.java
package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.sound.sampled.*;
import audio.AudioManager;

public class Client {
    private static final String AUDIO_FORMAT = "audio.wav"; // Formato de audio
    private Socket socket;
    private PrintWriter out;
    private AudioManager audioManager;

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {
        String host = "localhost"; // Dirección del servidor
        int serverPort = 12345; // Puerto del servidor
        Scanner scanner = new Scanner(System.in);

        try {
            // Crear un socket para conectarse al servidor (sin puerto fijo para evitar conflictos)
            socket = new Socket(host, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Usar un puerto dinámico para el AudioManager
            int localPort = socket.getLocalPort();
            audioManager = new AudioManager(localPort);

            System.out.println("Conectado al servidor en " + host + ":" + serverPort);

            // Leer el mensaje del servidor para ingresar el username
            String serverPrompt = in.readLine();
            System.out.println("Respuesta del servidor: " + serverPrompt);
            String username = scanner.nextLine();
            out.println(username);

            // Confirmación del servidor
            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            // Iniciar un hilo para leer mensajes entrantes del servidor
            new Thread(new ReadMessages(in)).start();

            // Bucle principal para enviar mensajes o comandos al servidor
            while (true) {
                System.out.println("\nEscribe tu mensaje:");
                System.out.println("'/privado username mensaje' para mensaje privado");
                System.out.println("'/grupo groupname mensaje' para mensaje de grupo");
                System.out.println("'/creargrupo groupname user1,user2,...' para crear un grupo");
                System.out.println("'/audio username|groupname' para enviar una nota de voz");
                System.out.println("'/llamada username|groupname' para realizar una llamada");
                System.out.println("'salir' para desconectarse\n");

                String message = scanner.nextLine();

                // Enviar audio
                if (message.startsWith("/audio ")) {
                    String target = message.split(" ", 2)[1];
                    InetAddress targetAddress = InetAddress.getByName(host);

                    // Grabar y enviar el audio
                    audioManager.startRecording(targetAddress, serverPort);
                    System.out.println("Grabando... Presiona ENTER para detener.");
                    scanner.nextLine(); // Esperar a que el usuario presione ENTER
                    audioManager.stopRecording();
                    System.out.println("Nota de voz enviada a " + target + ".");
                    out.println("/audio " + target);

                // Iniciar llamada (en desarrollo)
                } else if (message.startsWith("/llamada ")) {
                    out.println(message);
                    System.out.println("Iniciando llamada con " + message.split(" ", 2)[1]);

                // Otros comandos o mensajes generales
                } else {
                    out.println(message);
                }

                // Salir del cliente
                if (message.equalsIgnoreCase("salir")) {
                    System.out.println("Desconectando del servidor...");
                    break;
                }
            }

            socket.close();
            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para grabar audio en un archivo WAV
    private static File grabarAudio() {
        File audioFile = new File(AUDIO_FORMAT);
        AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info)) {
            microphone.open(format);
            AudioInputStream audioInputStream = new AudioInputStream(microphone);

            System.out.println("Grabando... Presiona ENTER para detener la grabación.");
            microphone.start();

            Thread stopper = new Thread(() -> {
                new Scanner(System.in).nextLine(); // Detener la grabación al presionar ENTER
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

    // Método para enviar un archivo al servidor mediante un socket
    private void enviarArchivo(File file, Socket socket) {
        try {
            byte[] buffer = new byte[4096];
            OutputStream os = socket.getOutputStream();
            FileInputStream fis = new FileInputStream(file);

            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            fis.close();
            System.out.println("Archivo enviado con éxito.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
