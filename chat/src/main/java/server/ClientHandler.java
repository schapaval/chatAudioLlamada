// Archivo: ClientHandler.java
package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String username;

    public String getUsername() {
        return username;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF("MESSAGE");
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAudio(File audioFile, String senderUsername) {
        try {
            out.writeUTF("AUDIO");
            out.writeUTF(senderUsername);
            out.writeUTF(audioFile.getName());
            out.writeLong(audioFile.length());
            out.flush();

            // Enviar los bytes del archivo
            FileInputStream fileIn = new FileInputStream(audioFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            fileIn.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF("Introduce tu nombre de usuario:");
            username = in.readUTF();
            Server.registerClient(username, this);
            out.writeUTF("Bienvenido, " + username + "!");

            String messageType;
            while ((messageType = in.readUTF()) != null) {
                if (messageType.equals("MESSAGE")) {
                    String message = in.readUTF();
                    // Procesar mensaje de texto
                    if (message.startsWith("/privado ")) {
                        // Enviar mensaje privado
                        String[] splitMessage = message.split(" ", 3);
                        if (splitMessage.length == 3) {
                            String recipient = splitMessage[1];
                            String privateMessage = splitMessage[2];
                            Server.sendPrivateMessage(recipient, privateMessage, this);
                        }
                    } else if (message.startsWith("/audio ")) {
                        // Enviar audio
                        String[] splitMessage = message.split(" ", 2);
                        if (splitMessage.length == 2) {
                            String target = splitMessage[1];
                            if (Server.getClients().containsKey(target)) {
                                sendMessage("Enviando nota de voz a " + target + "...");
                                // Recibir el archivo de audio del cliente
                                receiveAudioFile();
                                // Enviar el audio al destinatario
                                Server.sendPrivateAudio(target, new File("temp_audio.wav"), this);
                            } else {
                                sendMessage("Usuario " + target + " no encontrado.");
                            }
                        }
                    } else if (message.startsWith("/llamada ")) {
                        // Iniciar llamada
                        String[] splitMessage = message.split(" ", 2);
                        if (splitMessage.length == 2) {
                            String target = splitMessage[1];
                            Server.startCall(target, this);
                        }
                    } else {
                        // Enviar mensaje público
                        Server.broadcast(message, this);
                    }
                    // Guardar historial del mensaje del usuario
                    Server.saveMessageHistory(username, message);
                } else if (messageType.equals("AUDIO")) {
                    // Manejar recepción de audio (si es necesario)
                } else if (messageType.equals("CALL_ACCEPT")) {
                    String callerUsername = in.readUTF();
                    ClientHandler caller = Server.getClients().get(callerUsername);
                    if (caller != null) {
                        // Enviar información de conexión al llamante
                        caller.getOut().writeUTF("CALL_INFO");
                        caller.getOut().writeUTF(socket.getInetAddress().getHostAddress());
                        caller.getOut().writeInt(UDP_PORT); // Define un puerto UDP
                        caller.getOut().flush();
                    }
                } else if (messageType.equals("CALL_REJECT")) {
                    String callerUsername = in.readUTF();
                    ClientHandler caller = Server.getClients().get(callerUsername);
                    if (caller != null) {
                        caller.sendMessage("El usuario ha rechazado la llamada.");
                    }
                } else if (messageType.equals("EXIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                Server.removeClient(username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveAudioFile() {
        try {
            String fileName = in.readUTF();
            long fileSize = in.readLong();

            FileOutputStream fileOut = new FileOutputStream("temp_audio.wav");
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;
            while (totalBytesRead < fileSize && (bytesRead = in.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int UDP_PORT = 5000; // Puerto UDP para llamadas
}
