package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Map<String, ClientHandler> clients = new HashMap<>();

    public static void main(String[] args) {
        int port = 12345;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado en el puerto " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado.");
                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para enviar mensajes públicos
    public static synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients.values()) {
            if (client != sender) {
                client.sendMessage("Mensaje público de " + sender.getUsername() + ": " + message);
            }
        }
    }

    // Método para enviar mensajes privados
    public static synchronized void sendPrivateMessage(String username, String message, ClientHandler sender) {
        ClientHandler recipient = clients.get(username);
        if (recipient != null) {
            recipient.sendMessage("Mensaje privado de " + sender.getUsername() + ": " + message);
        } else {
            sender.sendMessage("Usuario " + username + " no encontrado.");
        }
    }

    // Método para enviar audios privados
    public static synchronized void sendPrivateAudio(String username, File audioFile, ClientHandler sender) {
        ClientHandler recipient = clients.get(username);
        if (recipient != null) {
            recipient.sendAudio(audioFile, sender.getUsername());
        } else {
            sender.sendMessage("Usuario " + username + " no encontrado.");
        }
    }

    // Método para registrar a los clientes
    public static synchronized void registerClient(String username, ClientHandler handler) {
        clients.put(username, handler);
    }

    // Método para eliminar clientes cuando se desconectan
    public static synchronized void removeClient(String username) {
        clients.remove(username);
    }
}

class ClientHandler extends Thread {
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
            out.println("AUDIO"); // Indicar que se enviará un archivo de audio
            out.println(senderUsername); // Enviar el nombre del remitente
            out.println(audioFile.getName()); // Enviar el nombre del archivo

            byte[] buffer = new byte[4096];
            try (FileInputStream fis = new FileInputStream(audioFile)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    this.socket.getOutputStream().write(buffer, 0, bytesRead); // Usar `this.socket`
                }
            }
            this.socket.getOutputStream().flush(); // Usar `this.socket`
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
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
                    // Mensaje privado: formato "/privado username mensaje"
                    String[] splitMessage = message.split(" ", 3);
                    if (splitMessage.length == 3) {
                        String recipient = splitMessage[1];
                        String privateMessage = splitMessage[2];
                        Server.sendPrivateMessage(recipient, privateMessage, this);
                    }
                } else if (message.startsWith("/privadoaudio ")) {
                    // Enviar audio privado: formato "/privadoaudio username"
                    String[] splitMessage = message.split(" ", 2);
                    if (splitMessage.length == 2) {
                        String recipient = splitMessage[1];
                        // Aquí puedes agregar la lógica para recibir y enviar archivos de audio
                        File audioFile = new File("audio.wav"); // Simulación
                        Server.sendPrivateAudio(recipient, audioFile, this);
                    }
                } else if (message.equals("salir")) {
                    break;
                } else {
                    // Mensaje público
                    Server.broadcast(message, this);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Server.removeClient(username);
        }
    }
}
