// Client.java
package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Client {
    private static final String AUDIO_FORMAT = "audio.wav";
    private Socket socket;
    private PrintWriter out;
    private ExecutorService executorService;
    private volatile boolean isRunning = true;

    //ip y puerto del servidor
    String host = "192.168.1.66";
    int port = 12345;

    public Client() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {
        Scanner scanner = new Scanner(System.in);
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Conectado al servidor en " + host + ":" + port);

            // Leer el mensaje del servidor para ingresar el username
            String serverPrompt = in.readLine();
            System.out.println("Respuesta del servidor: " + serverPrompt);

            String username = scanner.nextLine();
            out.println(username);

            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            // Iniciar el hilo para leer mensajes del servidor
            executorService.submit(new ReadMessages(in));

            while (isRunning) {
                printMenu();
                String message = scanner.nextLine();

                if (message.startsWith("/audio ")) {
                    handleAudioMessage(message);
                } else if (message.startsWith("/llamada ")) {
                    handleCallMessage(message);
                } else {
                    out.println(message);
                }

                if (message.equalsIgnoreCase("salir")) {
                    isRunning = false;
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void printMenu() {
        System.out.println("\nEscribe tu mensaje:");
        System.out.println("'/privado username mensaje' para mensaje privado");
        System.out.println("'/grupo groupname mensaje' para mensaje de grupo");
        System.out.println("'/creargrupo groupname user1,user2,...' para crear un grupo");
        System.out.println("'/audio username|groupname' para enviar una nota de voz");
        System.out.println("'/llamada username|groupname' para realizar una llamada");
        System.out.println("'salir' para desconectarse\n");
    }

    private void handleAudioMessage(String message) {
        String target = message.split(" ", 2)[1];
        executorService.submit(() -> {
            File audioFile = grabarAudio();
            if (audioFile != null) {
                out.println("/audio " + target);
                try {
                    Socket audioSocket = new Socket(host, port + 1);
                    sendAudioFile(audioFile, audioSocket);
                    System.out.println("Nota de voz enviada a " + target + ".");
                    audioSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleCallMessage(String message) {
        String target = message.split(" ", 2)[1];
        out.println(message);
        System.out.println("Llamada iniciada con " + target);
        executorService.submit(this::handleCall);
    }

    private void handleCall() {
        try {
            AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                return;
            }

            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);

            System.out.println("Llamada en progreso. Presiona ENTER para finalizar.");
            microphone.start();

            Socket callSocket = new Socket(host, port + 1);
            executorService.submit(() -> handleCallAudio(microphone, callSocket));

            // Esperar entrada del usuario para terminar la llamada
            new Scanner(System.in).nextLine();

            microphone.stop();
            microphone.close();
            callSocket.close();

        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCallAudio(TargetDataLine microphone, Socket socket) {
        try {
            OutputStream out = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            while (isRunning && socket.isConnected()) {
                int count = microphone.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAudioFile(File file, Socket socket) {
        try (FileInputStream fis = new FileInputStream(file)) {
            OutputStream os = socket.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File grabarAudio() {
        File audioFile = new File(AUDIO_FORMAT);
        try {
            AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Line not supported");
                return null;
            }

            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);

            System.out.println("Grabando... Presiona ENTER para detener.");
            microphone.start();

            AudioInputStream ais = new AudioInputStream(microphone);

            // Usar un thread separado para la grabación
            Future<?> recordingFuture = executorService.submit(() -> {
                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Esperar entrada del usuario
            new Scanner(System.in).nextLine();

            microphone.stop();
            microphone.close();
            recordingFuture.cancel(true);

            return audioFile;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void cleanup() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            executorService.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}