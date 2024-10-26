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

    String host = "localhost";
    int port = 12345;

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {

        Scanner scanner = new Scanner(System.in);
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(new AudioReceptor(socket)).start();
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
                        //metodo para el recibido de audio del cliente al que se le envia
                        recivedAudio(client2);
                    } else {
                        System.out.println("Error al grabar el audio.");
                    }
                } else if (message.startsWith("/llamada ")) {
                    out.println(message);
                    System.out.println("Llamada iniciada con " + message.split(" ", 2)[1]);
                    llamada(); //metodo para llamada entre clientes
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

    //llamada entre clientes
    private void llamada() {
        //llamada de el cliente que la empieza y el que la recibe
        try {
            AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            AudioInputStream audioInputStream = new AudioInputStream(microphone);

            System.out.println("Llamada iniciada. Presiona ENTER para finalizar la llamada.");
            microphone.start();

            Socket socket2 = new Socket(host, port+1);
            OutputStream os = socket2.getOutputStream();
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, os);

            Thread stopper = new Thread(() -> {
                new Scanner(System.in).nextLine(); // Detener la llamada cuando se presiona ENTER
                microphone.stop();
                microphone.close();
                try {
                    os.close();
                    socket2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            stopper.start();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }

    }

    //recibido de audio
    private void recivedAudio(Socket socket) {
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

    //metodo para reproducir audio
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
