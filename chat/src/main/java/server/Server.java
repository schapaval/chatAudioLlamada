// Archivo: Server.java
package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static Map<String, ClientHandler> clients = new HashMap<>();
    private static Map<String, Set<String>> groups = new HashMap<>(); // Grupos de chat

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

    // Método para enviar mensajes a un grupo
    public static synchronized void sendGroupMessage(String groupName, String message, ClientHandler sender) {
        Set<String> groupMembers = groups.get(groupName);
        if (groupMembers != null) {
            for (String memberUsername : groupMembers) {
                ClientHandler member = clients.get(memberUsername);
                if (member != null && member != sender) {
                    member.sendMessage("Mensaje de grupo (" + groupName + ") de " + sender.getUsername() + ": " + message);
                    saveMessageHistory(member.getUsername(), "Mensaje de grupo (" + groupName + ") de " + sender.getUsername() + ": " + message);
                }
            }
        } else {
            sender.sendMessage("Grupo " + groupName + " no encontrado.");
        }
    }

    // Método para enviar audios privados
    public static synchronized void sendPrivateAudio(String recipientUsername, File audioFile, ClientHandler sender, Socket clientToSend) {
        ClientHandler recipient = clients.get(recipientUsername);
        if (recipient != null) {
            recipient.sendAudio(audioFile, sender.getUsername(), clientToSend);
            saveAudioHistory(recipient.getUsername(), audioFile);
        } else {
            sender.sendMessage("Usuario " + recipientUsername + " no encontrado.");
        }
    }

    // Método para enviar audios a un grupo
    public static synchronized void sendGroupAudio(String groupName, File audioFile, ClientHandler sender) {
        Set<String> groupMembers = groups.get(groupName);
        if (groupMembers != null) {
            for (String memberUsername : groupMembers) {
                ClientHandler member = clients.get(memberUsername);
                if (member != null && member != sender) {
                    member.sendAudio(audioFile, sender.getUsername(), null);
                    saveAudioHistory(member.getUsername(), audioFile);
                }
            }
        } else {
            sender.sendMessage("Grupo " + groupName + " no encontrado.");
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

    // Método para crear un grupo
    public static synchronized void createGroup(String groupName, Set<String> usernames, ClientHandler creator) {
        groups.put(groupName, usernames);
        creator.sendMessage("Grupo '" + groupName + "' creado exitosamente.");
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
    public static synchronized void startCall(String target, ClientHandler caller) {
        if (clients.containsKey(target)) {
            ClientHandler targetClient = clients.get(target);
            caller.sendMessage("Iniciando llamada con " + target + "...");
            targetClient.sendMessage("Llamada entrante de " + caller.getUsername() + ". ¿Deseas contestar? (s/n)");

        } else {
            caller.sendMessage("Usuario " + target + " no encontrado.");
        }

    }



    // Método para contestar una llamada
    public static void answerCall(String username, String caller) {
        ClientHandler callerHandler = clients.get(caller);
        if (callerHandler != null) {
            callerHandler.sendMessage(username + " ha contestado la llamada.");
            // Iniciar hilo para grabar constantemente audio y enviarlos al cliente que nos hizo la llamada

            new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(12346);
                    Socket socket = serverSocket.accept();
                    System.out.println("Conexión establecida con " + caller);
                    callerHandler.sendMessage("Conexión establecida con " + username);
                    callerHandler.setSocket(socket);
                    callerHandler.startRecording();
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            //escuchar la del cliente que nos hizo la llamada
            new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(12347);
                    Socket socket = serverSocket.accept();
                    System.out.println("Conexión establecida con " + caller);
                    callerHandler.sendMessage("Conexión establecida con " + username);
                    callerHandler.setSocket(socket);
                    callerHandler.startListening();
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            //para terminar la llamada cuando escriban colgar
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.equalsIgnoreCase("colgar")) {
                            callerHandler.stopRecording();
                            callerHandler.stopListening();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();


        }
        else {
            System.out.println("Cliente no encontrado");
        }


    }
}
