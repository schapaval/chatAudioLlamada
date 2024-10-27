// Archivo: Client.java
package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.sound.sampled.*;

public class Client {
    private static final String AUDIO_FORMAT = "audio.wav";
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean isInCall = false;
    private int udpPort = 6000; // Puerto UDP para recibir audio

    // IP y puerto del servidor
    String host = "192.168.29.130";
    int port = 12345;

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {
        Scanner scanner = new Scanner(System.in);
        try {
            socket = new Socket(host, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            System.out.println("Conectado al servidor en " + host + ":" + port);

            // Leer el mensaje del servidor para ingresar el username
            String serverPrompt = in.readUTF();
            System.out.println("Respuesta del servidor: " + serverPrompt);

            String username = scanner.nextLine();
            out.writeUTF(username);

            String response = in.readUTF();
            System.out.println("Respuesta del servidor: " + response);

            // Iniciar el hilo para leer mensajes del servidor
            new Thread(new ReadMessages(in)).start();

            while (true) {
                printMenu();
                String message = scanner.nextLine();

                if (message.startsWith("/audio ")) {
                    handleAudioMessage(message);
                } else if (message.startsWith("/llamada ")) {
                    handleCallMessage(message);
                } else if (message.equalsIgnoreCase("salir")) {
                    out.writeUTF("EXIT");
                    break;
                } else {
                    out.writeUTF("MESSAGE");
                    out.writeUTF(message);
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
        System.out.println("'/audio username' para enviar una nota de voz");
        System.out.println("'/llamada username' para realizar una llamada");
        System.out.println("'salir' para desconectarse\n");
    }

    private void handleAudioMessage(String message) {
        String target = message.split(" ", 2)[1];
        File audioFile = grabarAudio();
        if (audioFile != null) {
            try {
                out.writeUTF("MESSAGE");
                out.writeUTF("/audio " + target);
                out.writeUTF(audioFile.getName());
                out.writeLong(audioFile.length());
                FileInputStream fileIn = new FileInputStream(audioFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
                fileIn.close();
                System.out.println("Nota de voz enviada a " + target + ".");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCallMessage(String message) {
        String target = message.split(" ", 2)[1];
        try {
            out.writeUTF("MESSAGE");
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File grabarAudio() {
        File audioFile = new File(AUDIO_FORMAT);
        try {
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Línea no soportada");
                return null;
            }

            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);

            System.out.println("Grabando... Presiona ENTER para detener.");
            microphone.start();

            AudioInputStream ais = new AudioInputStream(microphone);

            // Usar un thread separado para la grabación
            Thread recordingThread = new Thread(() -> {
                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            recordingThread.start();

            // Esperar entrada del usuario
            new Scanner(System.in).nextLine();

            microphone.stop();
            microphone.close();

            return audioFile;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna para leer mensajes del servidor
    private class ReadMessages implements Runnable {
        private DataInputStream in;

        public ReadMessages(DataInputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String messageType = in.readUTF();
                    if (messageType.equals("MESSAGE")) {
                        String message = in.readUTF();
                        System.out.println("\n" + message);
                    } else if (messageType.equals("AUDIO")) {
                        String senderUsername = in.readUTF();
                        String fileName = in.readUTF();
                        long fileSize = in.readLong();

                        File audioFile = new File("received_" + fileName);
                        FileOutputStream fileOut = new FileOutputStream(audioFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesRead = 0;
                        while (totalBytesRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                            fileOut.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        fileOut.close();

                        System.out.println("\nHas recibido una nota de voz de " + senderUsername);
                        playAudio(audioFile);
                    } else if (messageType.equals("CALL_REQUEST")) {
                        String callerUsername = in.readUTF();
                        System.out.println("\nLlamada entrante de " + callerUsername + ". ¿Aceptar? (s/n)");
                        String response = new Scanner(System.in).nextLine();
                        if (response.equalsIgnoreCase("s")) {
                            out.writeUTF("CALL_ACCEPT");
                            out.writeUTF(callerUsername);
                            out.flush();
                            isInCall = true;
                            startAudioReceiving();
                        } else {
                            out.writeUTF("CALL_REJECT");
                            out.writeUTF(callerUsername);
                            out.flush();
                        }
                    } else if (messageType.equals("CALL_INFO")) {
                        String targetIP = in.readUTF();
                        int targetPort = in.readInt();
                        isInCall = true;
                        startAudioSending(targetIP, targetPort);
                    } else if (messageType.equals("CALL_END")) {
                        isInCall = false;
                        System.out.println("\nLa llamada ha terminado.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Desconectado del servidor.");
            }
        }

        private void playAudio(File audioFile) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
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
    }

    private void startAudioSending(String targetIP, int targetPort) {
        new Thread(() -> {
            try {
                DatagramSocket udpSocket = new DatagramSocket();
                InetAddress targetAddress = InetAddress.getByName(targetIP);
                AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                byte[] buffer = new byte[4096];
                System.out.println("Enviando audio...");
                while (isInCall) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, targetAddress, targetPort);
                    udpSocket.send(packet);
                }
                microphone.close();
                udpSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startAudioReceiving() {
        new Thread(() -> {
            try {
                DatagramSocket udpSocket = new DatagramSocket(udpPort);
                AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();
                byte[] buffer = new byte[4096];
                System.out.println("Recibiendo audio...");
                while (isInCall) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    speakers.write(packet.getData(), 0, packet.getLength());
                }
                speakers.close();
                udpSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
