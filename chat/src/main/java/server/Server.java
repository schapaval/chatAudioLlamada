// Archivo: Server.java
package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

public class Server {
    private static final int PORT = 12345;
    private static final int AUDIO_PORT = 50000;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> groups = new ConcurrentHashMap<>();
    private static DatagramSocket audioSocket;
    public static Map<String, ClientHandler> getClients() {
        return clients;
    }

    public static Map<String, Set<String>> getGroups() {
        return groups;
    }


    public static void main(String[] args) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            System.out.println("Available mixer: " + mixerInfo.getName());
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                System.out.println("Source line: " + lineInfo);
            }
            for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                System.out.println("Target line: " + lineInfo);
            }
        }
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            audioSocket = new DatagramSocket(AUDIO_PORT);

            System.out.println("Servidor iniciado en el puerto " + PORT);

            // Iniciar el hilo para manejar el audio
            new Thread(() -> handleAudio()).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado.");
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void handleAudio() {
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                audioSocket.receive(packet);

                // Reenviar el audio a todos los clientes conectados excepto al remitente
                for (ClientHandler client : clients.values()) {
                    if (!client.getAddress().equals(packet.getAddress())) {
                        DatagramPacket forwardPacket = new DatagramPacket(
                                packet.getData(),
                                packet.getLength(),
                                client.getAddress(),
                                client.getAudioPort()
                        );
                        audioSocket.send(forwardPacket);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    public static synchronized void sendPrivateAudio(String recipientUsername, File audioFile, ClientHandler sender) {
        ClientHandler recipient = clients.get(recipientUsername);
        if (recipient != null) {
            recipient.sendAudio(audioFile, sender.getUsername());
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
                    member.sendAudio(audioFile, sender.getUsername());
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




}
