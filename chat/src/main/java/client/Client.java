package client;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String AUDIO_FORMAT = "audio.wav";

    public static void main(String[] args) {
        String host = "localhost"; // Cambia a la IP del servidor si es remoto
        int port = 12345; // Puerto al que el cliente se conectará
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

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
                System.out.println("\nEscribe tu mensaje (o usa '/privado username mensaje' para mensaje privado, '/privadoaudio username' para audio privado, 'audio' para mensaje de voz público):");
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("audio")) {
                    // Grabar y enviar audio público
                    File audioFile = grabarAudio();
                    if (audioFile != null) {
                        enviarArchivo(audioFile, socket);
                        System.out.println("Mensaje de audio público enviado.");
                    } else {
                        System.out.println("Error al grabar el audio.");
                    }
                } else if (message.startsWith("/privadoaudio ")) {
                    // Enviar audio privado
                    File audioFile = grabarAudio();
                    if (audioFile != null) {
                        out.println(message);  // Envía el comando al servidor
                        enviarArchivo(audioFile, socket);
                        System.out.println("Mensaje de audio privado enviado.");
                    } else {
                        System.out.println("Error al grabar el audio.");
                    }
                } else {
                    out.println(message);  // Envía el mensaje (público o privado)
                    guardarHistorial(message);

                    if (message.equalsIgnoreCase("salir")) {
                        System.out.println("Desconectando del servidor...");
                        break;  // Sale del ciclo cuando el cliente escribe "salir"
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para grabar audio
    private static File grabarAudio() {
        File audioFile = new File(AUDIO_FORMAT);
        AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        try (TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info)) {
            microphone.open(format);
            AudioInputStream audioInputStream = new AudioInputStream(microphone);

            System.out.println("Grabando... Presiona ENTER para detener la grabación.");
            microphone.start();

            new Thread(() -> {
                new Scanner(System.in).nextLine(); // Detener con ENTER
                microphone.stop();
                microphone.close();
            }).start();

            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, audioFile);
            System.out.println("Grabación finalizada.");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return audioFile;
    }

    // Método para enviar un archivo al servidor
    private static void enviarArchivo(File file, Socket socket) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             OutputStream os = socket.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) > 0) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para guardar el historial de mensajes
    private static void guardarHistorial(String mensaje) {
        try (FileWriter fw = new FileWriter("historial.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(mensaje);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Clase para manejar los mensajes entrantes del servidor
class ReadMessages implements Runnable {
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
            }
        } catch (IOException e) {
            System.out.println("Error leyendo mensajes del servidor.");
        }
    }
}
