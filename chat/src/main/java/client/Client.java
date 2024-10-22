package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.sound.sampled.*;

public class Client {
    private static final String AUDIO_FORMAT = "audio.wav";
    private Socket socket;
    private PrintWriter out;
    private boolean inCall = false;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {
        String host = "192.168.50.130";
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
            new Thread(new ReadMessages(in, this)).start();

            // Hilo principal para enviar mensajes al servidor
            while (true) {
                System.out.println("\nEscribe tu mensaje:");
                System.out.println("'/privado username mensaje' para mensaje privado");
                System.out.println("'/grupo groupname mensaje' para mensaje de grupo");
                System.out.println("'/creargrupo groupname user1,user2,...' para crear un grupo");
                System.out.println("'/audio username|groupname' para enviar una nota de voz");
                System.out.println("'/llamada username|groupname' para realizar una llamada");
                System.out.println("'/finllamada' para finalizar una llamada activa");
                System.out.println("'/aceptar' para aceptar una llamada entrante");
                System.out.println("'/rechazar' para rechazar una llamada entrante");
                System.out.println("'salir' para desconectarse\n");

                String message = scanner.nextLine();

                if (message.startsWith("/audio ")) {
                    String target = message.split(" ", 2)[1];
                    File audioFile = grabarAudio();
                    if (audioFile != null) {
                        out.println("/audio " + target);
                        enviarArchivo(audioFile, socket);
                        System.out.println("Nota de voz enviada a " + target + ".");
                    } else {
                        System.out.println("Error al grabar el audio.");
                    }
                } else if (message.startsWith("/llamada ")) {
                    if (!inCall) {
                        out.println(message);
                        System.out.println("Esperando respuesta...");
                    } else {
                        System.out.println("Ya estás en una llamada. Finaliza la llamada actual antes de iniciar una nueva.");
                    }
                } else if (message.equals("/finllamada")) {
                    if (inCall) {
                        endCall();
                        out.println("/finllamada");
                    } else {
                        System.out.println("No estás en una llamada actualmente.");
                    }
                } else if (message.equals("/aceptar")) {
                    if (!inCall) {
                        out.println("/aceptar");
                        startCall();
                    } else {
                        System.out.println("Ya estás en una llamada.");
                    }
                } else if (message.equals("/rechazar")) {
                    out.println("/rechazar");
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

    private void enviarArchivo(File file, Socket socket) {
        try {
            byte[] buffer = new byte[4096]; // Tamaño del buffer
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

    public void startCall() throws IOException {
        inCall = true;
        System.out.println("Llamada iniciada. Hablando...");
        
        AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        
        try {
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            OutputStream out = socket.getOutputStream();
            AudioInputStream ais = new AudioInputStream(microphone);

            new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (inCall && (ais.read(buffer) > 0)) {
                        out.write(buffer);
                        out.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    InputStream in = socket.getInputStream();
                    sourceDataLine = (SourceDataLine) AudioSystem.getLine(
                        new DataLine.Info(SourceDataLine.class, format));
                    sourceDataLine.open(format);
                    sourceDataLine.start();
                    while (inCall && (in.read(buffer) > 0)) {
                        sourceDataLine.write(buffer, 0, buffer.length);
                    }
                } catch (IOException | LineUnavailableException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void endCall() {
        inCall = false;
        System.out.println("Llamada finalizada.");
        if (sourceDataLine != null) {
            sourceDataLine.stop();
            sourceDataLine.close();
        }
    }

    public void handleIncomingCall(String caller) {
        System.out.println("\nLlamada entrante de " + caller);
        System.out.println("Escribe '/aceptar' para aceptar la llamada o '/rechazar' para rechazarla.");
    }

    public boolean isInCall() {
        return inCall;
    }

    public void setInCall(boolean inCall) {
        this.inCall = inCall;
    }
}