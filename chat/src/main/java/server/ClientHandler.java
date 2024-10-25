package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private String username;

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.dataIn = new DataInputStream(socket.getInputStream());
            this.dataOut = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Solicitar el nombre de usuario al cliente
            out.println("Introduce tu nombre de usuario:");
            username = in.readLine();
            if (username == null) {
                socket.close();
                return;
            }

            // Registrar al cliente
            Server.registerClient(username, this);
            out.println("Bienvenido, " + username + "!");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/audio ")) {
                    String[] splitMessage = message.split(" ", 2);
                    if (splitMessage.length == 2) {
                        String target = splitMessage[1];
                        // Recibir el archivo de audio del remitente
                        File audioFile = recibirArchivo();
                        if (audioFile != null) {
                            if (Server.getClients().containsKey(target)) {
                                // Enviar el archivo al destinatario
                                Server.sendPrivateAudio(target, audioFile, this);
                            } else {
                                sendMessage("Usuario " + target + " no encontrado.");
                            }
                        } else {
                            sendMessage("Error al recibir el archivo de audio.");
                        }
                    }
                } else if (message.startsWith("/privado ")) {
                    // Lógica para mensajes privados
                    String[] splitMessage = message.split(" ", 3);
                    if (splitMessage.length == 3) {
                        String recipient = splitMessage[1];
                        String privateMessage = splitMessage[2];
                        Server.sendPrivateMessage(recipient, privateMessage, this);
                    }
                } else if (message.equals("salir")) {
                    break;
                } else {
                    // Enviar mensaje público
                    Server.broadcast(message, this);
                }

                // Guardar historial del mensaje del usuario
                Server.saveMessageHistory(username, message);
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

    // Método para recibir el archivo de audio del remitente
    private File recibirArchivo() {
        try {
            // Leer el tamaño del archivo primero
            long fileSize = dataIn.readLong();

            // Crear un archivo temporal para almacenar el audio
            File audioFile = new File("temp_" + System.currentTimeMillis() + ".wav");
            FileOutputStream fos = new FileOutputStream(audioFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while (totalBytesRead < fileSize &&
                    (bytesRead = dataIn.read(buffer, 0,
                            (int)Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            fos.close();

            if (totalBytesRead == fileSize) {
                System.out.println("Archivo de audio recibido correctamente del usuario " + username);
                return audioFile;
            } else {
                System.out.println("Error al recibir el archivo de audio del usuario " + username);
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Método para enviar el archivo de audio al cliente destinatario
    public void enviarArchivo(File audioFile, String senderUsername) {
        try {
            // Enviar el mensaje de notificación
            sendMessage("Has recibido una nota de voz de " + senderUsername);

            // Enviar el tamaño del archivo primero
            dataOut.writeLong(audioFile.length());

            // Enviar el contenido del archivo
            FileInputStream fis = new FileInputStream(audioFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
            dataOut.flush();
            fis.close();
            System.out.println("Archivo de audio enviado al cliente " + username);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error al enviar el archivo de audio al cliente " + username);
        }
    }
}
