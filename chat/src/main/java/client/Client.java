package client;

import audio.PlayerThread;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.sound.sampled.*;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;  // Para los mensajes de texto
    private volatile boolean llamadaActiva = false;
    private DatagramSocket audioSocket;
    private static final int AUDIO_PORT = 50000;
    private static final int BUFFER_SIZE = 1024;
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private Thread audioSendThread;
    private Thread audioReceiveThread;
    private PlayerThread playerThread;

    public static void main(String[] args) {
        new Client().startClient();
    }

    private void startClient() {
        String host = "localhost";
        int port = 12345;
        Scanner scanner = new Scanner(System.in);

        try {
            socket = new Socket(host, port);
            audioSocket = new DatagramSocket();
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Conectado al servidor en " + host + ":" + port);

            // Inicializar el PlayerThread para reproducir audio automáticamente
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            playerThread = new PlayerThread(format, BUFFER_SIZE);
            playerThread.start();

            // Leer el mensaje del servidor para ingresar el username
            String serverPrompt = in.readLine();
            System.out.println("Respuesta del servidor: " + serverPrompt);

            // Enviar el username al servidor
            String username = scanner.nextLine();
            out.println(username);

            // Esperar la bienvenida del servidor
            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            // Iniciar el hilo para leer mensajes de texto del servidor
            new Thread(() -> readMessages()).start();

            // Iniciar el hilo para recibir datos de audio
            new Thread(() -> receiveAudio()).start();

            // Hilo principal para enviar mensajes al servidor
            while (true) {
                mostrarMenu();
                String message = scanner.nextLine();

                if (message.startsWith("/llamada ")) {
                    handleLlamada(message);
                } else if (message.equals("colgar")) {
                    stopLlamada();
                } else {
                    out.println(message);
                }

                if (message.equals("salir")) {
                    stopLlamada();
                    break;
                }
            }

            cleanup();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Mensaje recibido: " + message);

                // Si recibimos un audio, solo mostramos el mensaje.
                if (message.contains("Has recibido una nota de voz")) {
                    System.out.println("Audio recibido, reproduciéndolo en breve...");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveAudio() {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                audioSocket.receive(packet);
                playerThread.addBytes(packet.getData());  // Agregar los bytes al PlayerThread para reproducir el audio
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeAudio() {
        try {
            // Configurar formato de audio
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);

            // Configurar micrófono
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            microphone.open(format);

            // Configurar altavoces
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
            speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speakers.open(format);

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void handleLlamada(String message) {
        if (!llamadaActiva) {
            String target = message.split(" ", 2)[1];
            out.println(message);
            startLlamada();
        } else {
            System.out.println("Ya hay una llamada activa.");
        }
    }

    private void startLlamada() {
        try {
            llamadaActiva = true;
            microphone.start();
            speakers.start();

            // Hilo para enviar audio
            audioSendThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    InetAddress serverAddress = InetAddress.getLocalHost();

                    while (llamadaActiva) {
                        int count = microphone.read(buffer, 0, buffer.length);
                        if (count > 0) {
                            DatagramPacket packet = new DatagramPacket(buffer, count, serverAddress, AUDIO_PORT);
                            audioSocket.send(packet);
                        }
                    }
                } catch (Exception e) {
                    if (llamadaActiva) {
                        e.printStackTrace();
                    }
                }
            });

            // Hilo para recibir audio
            audioReceiveThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while (llamadaActiva) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        audioSocket.receive(packet);
                        speakers.write(packet.getData(), 0, packet.getLength());
                    }
                } catch (Exception e) {
                    if (llamadaActiva) {
                        e.printStackTrace();
                    }
                }
            });

            audioSendThread.start();
            audioReceiveThread.start();

            System.out.println("Llamada iniciada. Escribe 'colgar' para terminar.");

        } catch (Exception e) {
            e.printStackTrace();
            stopLlamada();
        }
    }

    private void stopLlamada() {
        if (llamadaActiva) {
            llamadaActiva = false;

            if (microphone != null) {
                microphone.stop();
                microphone.flush();
            }

            if (speakers != null) {
                speakers.stop();
                speakers.flush();
            }

            try {
                if (audioSendThread != null) {
                    audioSendThread.join(1000);
                }
                if (audioReceiveThread != null) {
                    audioReceiveThread.join(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Llamada finalizada.");
        }
    }

    private void cleanup() {
        try {
            stopLlamada();

            if (microphone != null) {
                microphone.close();
            }
            if (speakers != null) {
                speakers.close();
            }
            if (audioSocket != null) {
                audioSocket.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mostrarMenu() {
        System.out.println("\nEscribe tu mensaje:");
        System.out.println("'/privado username mensaje' para mensaje privado");
        System.out.println("'/grupo groupname mensaje' para mensaje de grupo");
        System.out.println("'/creargrupo groupname user1,user2,...' para crear un grupo");
        System.out.println("'/audio username|groupname' para enviar una nota de voz");
        System.out.println("'/llamada username|groupname' para realizar una llamada");
        System.out.println("'colgar' para finalizar la llamada");
        System.out.println("'salir' para desconectarse\n");
    }
}
