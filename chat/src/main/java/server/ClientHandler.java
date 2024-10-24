package server;

import java.io.*;
import java.net.*;
import java.util.*;

import audio.AudioManager;
import audio.CallManager;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private AudioManager audioManager;
    private static final CallManager callManager;

    // Bloque estático para inicializar el CallManager
    static {
        callManager = new CallManager(50000); // Base port para las llamadas
    }

    // Constructor que acepta un Socket y un puerto base de audio
    public ClientHandler(Socket socket, int baseAudioPort) {
        this.socket = socket;
        try {
            // Generar un puerto dinámico para evitar conflictos
            int dynamicPort = baseAudioPort + new Random().nextInt(1000);
            this.audioManager = new AudioManager(dynamicPort); // Inicializa el AudioManager
            this.audioManager.startPlaying(); // Inicia la reproducción de audio

            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void receiveAudio(byte[] audioData) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(audioData);
            os.flush();
            sendMessage("Audio recibido y reproducido correctamente.");
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
                    handlePrivateMessage(message);
                } else if (message.startsWith("/grupo ")) {
                    handleGroupMessage(message);
                } else if (message.startsWith("/creargrupo ")) {
                    handleCreateGroup(message);
                } else if (message.startsWith("/audio ")) {
                    handleAudioMessage(message);
                } else if (message.startsWith("/llamada ")) {
                    handleCall(message.split(" ", 2)[1]);
                } else if (message.equals("salir")) {
                    break;
                } else {
                    Server.broadcast(message, this);
                }
                Server.saveMessageHistory(username, message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void handlePrivateMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        if (splitMessage.length == 3) {
            String recipient = splitMessage[1];
            String privateMessage = splitMessage[2];
            Server.sendPrivateMessage(recipient, privateMessage, this);
        }
    }

    private void handleGroupMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        if (splitMessage.length == 3) {
            String groupName = splitMessage[1];
            String groupMessage = splitMessage[2];
            Server.sendGroupMessage(groupName, groupMessage, this);
        }
    }

    private void handleCreateGroup(String message) {
        String[] splitMessage = message.split(" ", 3);
        if (splitMessage.length == 3) {
            String groupName = splitMessage[1];
            String[] members = splitMessage[2].split(",");
            Set<String> memberSet = new HashSet<>(Arrays.asList(members));
            memberSet.add(username);
            Server.createGroup(groupName, memberSet, this);
        }
    }

    private void handleAudioMessage(String message) {
        String[] splitMessage = message.split(" ", 2);
        if (splitMessage.length == 2) {
            String target = splitMessage[1];
            byte[] audioData = new byte[4096];
            if (Server.getClients().containsKey(target)) {
                Server.sendPrivateAudio(target, this, audioData);
            } else if (Server.getGroups().containsKey(target)) {
                Server.sendGroupAudio(target, this, audioData);
            } else {
                sendMessage("Usuario o grupo " + target + " no encontrado.");
            }
        }
    }

    private void handleCall(String target) {
        if (target.startsWith("grupo:")) {
            String groupName = target.substring(6);
            Set<String> participants = Server.getGroups().get(groupName);
            if (participants != null) {
                startGroupCall(groupName, participants);
            } else {
                sendMessage("Grupo no encontrado: " + groupName);
            }
        } else {
            ClientHandler targetClient = Server.getClients().get(target);
            if (targetClient != null) {
                startPrivateCall(target);
            } else {
                sendMessage("Usuario no encontrado: " + target);
            }
        }
    }

    private void startPrivateCall(String target) {
        Set<String> participants = new HashSet<>(Arrays.asList(username, target));
        String callId = UUID.randomUUID().toString();

        Map<String, InetAddress> addressMap = new HashMap<>();
        addressMap.put(username, socket.getInetAddress());
        addressMap.put(target, Server.getClients().get(target).socket.getInetAddress());

        callManager.startCall(callId, username, participants, addressMap);
        sendMessage("Llamada iniciada con " + target);
        Server.getClients().get(target).sendMessage("Llamada entrante de " + username);
    }

    private void startGroupCall(String groupName, Set<String> participants) {
        String callId = "group-" + UUID.randomUUID().toString();

        Map<String, InetAddress> addressMap = new HashMap<>();
        for (String participant : participants) {
            ClientHandler client = Server.getClients().get(participant);
            if (client != null) {
                addressMap.put(participant, client.socket.getInetAddress());
            }
        }
        callManager.startCall(callId, username, participants, addressMap);
        for (String participant : participants) {
            if (!participant.equals(username)) {
                Server.getClients().get(participant)
                        .sendMessage("Llamada grupal iniciada en " + groupName + " por " + username);
            }
        }
    }

    private void closeConnection() {
        try {
            socket.close();
            Server.removeClient(username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
