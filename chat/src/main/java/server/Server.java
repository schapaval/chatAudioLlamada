// Archivo: Server.java
package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Map<String, ClientHandler> clients = new HashMap<>();
    private static Map<String, Set<String>> groups = new HashMap<>();

    public static Map<String, ClientHandler> getClients() {
        return clients;
    }

    public static Map<String, Set<String>> getGroups() {
        return groups;
    }

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
                saveMessageHistory(client.getUsername(), "Mensaje público de " + sender.getUsername() + ": " + message);
            }
        }
    }

    // Método para enviar mensajes privados
    public static synchronized void sendPrivateMessage(String recipientUsername, String message, ClientHandler sender) {
        ClientHandler recipient = clients.get(recipientUsername);
        if (recipient != null) {
            recipient.sendMessage("Mensaje privado de " + sender.getUsername() + ": " + message);
            saveMessageHistory(recipient.getUsername(), "Mensaje privado de " + sender.getUsername() + ": " + message);
        } else {
            sender.sendMessage("Usuario " + recipientUsername + " no encontrado.");
        }
    }

    // Método para enviar audios privados
    public static synchronized void sendPrivateAudio(String recipientUsername, File audioFile, ClientHandler sender) {
        ClientHandler recipient = clients.get(recipientUsername);
        if (recipient != null) {
            recipient.sendAudio(audioFile, sender.getUsername());
            saveAudioHistory(recipient.getUsername(), audioFile);
        } else {
            sender.sendMessage("Usuario " + recipientUsername + " no encontrado.");
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

    // Método para guardar el historial de mensajes
    public static synchronized void saveMessageHistory(String username, String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(username + "_historial.txt", true))) {
            writer.write("[" + new Date() + "] " + message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para guardar el historial de audios
    public static synchronized void saveAudioHistory(String username, File audioFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(username + "_historial.txt", true))) {
            writer.write("[" + new Date() + "] Audio recibido: " + audioFile.getName() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para iniciar una llamada
    public static synchronized void startCall(String targetUsername, ClientHandler caller) {
        ClientHandler targetClient = clients.get(targetUsername);
        if (targetClient != null) {
            try {
                // Enviar solicitud de llamada al cliente objetivo
                targetClient.getOut().writeUTF("CALL_REQUEST");
                targetClient.getOut().writeUTF(caller.getUsername());
                targetClient.getOut().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            caller.sendMessage("Usuario " + targetUsername + " no encontrado.");
        }
    }
}
