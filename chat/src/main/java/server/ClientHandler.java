package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendAudio(File audioFile, String senderUsername) {
        try {
            out.println("Has recibido una nota de voz de " + senderUsername + ": " + audioFile.getName());

            byte[] buffer = new byte[4096];
            try (FileInputStream fis = new FileInputStream(audioFile)) {
                int bytesRead;
                OutputStream os = socket.getOutputStream();
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            socket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Introduce tu nombre de usuario:");
            username = in.readLine();
            Server.registerClient(username, this);
            out.println("Bienvenido, " + username + "!");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/privado ")) {
                    // Enviar mensaje privado
                    String[] splitMessage = message.split(" ", 3);
                    if (splitMessage.length == 3) {
                        String recipient = splitMessage[1];
                        String privateMessage = splitMessage[2];
                        Server.sendPrivateMessage(recipient, privateMessage, this);
                    }
                } else if (message.startsWith("/grupo ")) {
                    // Enviar mensaje a grupo
                    String[] splitMessage = message.split(" ", 3);
                    if (splitMessage.length == 3) {
                        String groupName = splitMessage[1];
                        String groupMessage = splitMessage[2];
                        Server.sendGroupMessage(groupName, groupMessage, this);
                    }
                } else if (message.startsWith("/creargrupo ")) {
                    // Crear grupo
                    String[] splitMessage = message.split(" ", 3);
                    if (splitMessage.length == 3) {
                        String groupName = splitMessage[1];
                        String[] members = splitMessage[2].split(",");
                        Set<String> memberSet = new HashSet<>(Arrays.asList(members));
                        memberSet.add(username); // Agregar al creador al grupo
                        Server.createGroup(groupName, memberSet, this);
                    }
                } else if (message.startsWith("/audio ")) {
                    // Enviar audio (implementación simplificada)
                    String[] splitMessage = message.split(" ", 2);
                    if (splitMessage.length == 2) {
                        String target = splitMessage[1];
                        File audioFile = new File("audio.wav"); // Simulación del archivo de audio
                        if (Server.getClients().containsKey(target)) {
                            Server.sendPrivateAudio(target, audioFile, this);
                        } else if (Server.getGroups().containsKey(target)) {
                            Server.sendGroupAudio(target, audioFile, this);
                        } else {
                            sendMessage("Usuario o grupo " + target + " no encontrado.");
                        }
                    }
                } else if (message.startsWith("/llamada ")) {
                    // Iniciar llamada (implementación simplificada)
                    String[] splitMessage = message.split(" ", 2);
                    if (splitMessage.length == 2) {
                        String target = splitMessage[1];
                        if (Server.getClients().containsKey(target)) {
                            sendMessage("Iniciando llamada con " + target + "...");
                            // Aquí puedes implementar la funcionalidad de llamadas.
                        } else if (Server.getGroups().containsKey(target)) {
                            sendMessage("Iniciando llamada en grupo " + target + "...");
                            // Aquí puedes implementar la funcionalidad de llamadas en grupo.
                        } else {
                            sendMessage("Usuario o grupo " + target + " no encontrado.");
                        }
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
}