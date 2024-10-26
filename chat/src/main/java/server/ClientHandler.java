package server;

import java.io.*;
import java.net.*;
import java.util.*;

import audio.AudioManager;
import audio.CallManager;
import audio.AudioManager;
import audio.CallManager;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private AudioManager audioManager;
    private static final CallManager callManager;

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendAudio(File audioFile, String senderUsername, Socket socket) {
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

            for (int i = 0; i < 10; i++){
                System.out.println("ganemos tiempo");
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
                        if (Server.getClients().containsKey(target)) {
                            sendMessage("Enviando nota de voz a " + target + "...");
                            Server.getClients().get(target).sendAudio(new File("audio.wav"), username, socket);
                            Server.sendPrivateAudio(target, new File("audio.wav"), this, socket);

                        } else if (Server.getGroups().containsKey(target)) {
                            sendMessage("Enviando nota de voz al grupo " + target + "...");

                            Server.getGroups().get(target).forEach(member -> {
                                if (!member.equals(username)) {
                                    Server.getClients().get(member).sendAudio(new File("audio.wav"), username, socket);
                                    Server.sendPrivateAudio(member, new File("audio.wav"), this, socket);
                                }
                            });

                        } else {
                            sendMessage("Usuario o grupo " + target + " no encontrado.");
                        }
                    }
                    // para cuando recibo un audio del otro cliente
                } else if (message.startsWith("Has recibido una nota")) {
                    // Reproducir audio
                    audioManager.playAudio("received_audio.wav");

                } else if (message.startsWith("/llamada ")) {
                    // Iniciar llamada (implementación simplificada)
                    String[] splitMessage = message.split(" ", 2);
                    if (splitMessage.length == 2) {
                        String target = splitMessage[1];
                        if (Server.getClients().containsKey(target)) {
                            sendMessage("Iniciando llamada con " + target + "...");
                            Server.startCall(target, this);

                        } else if (Server.getGroups().containsKey(target)) {
                            sendMessage("Iniciando llamada en grupo " + target + "...");
                            Server.getGroups().get(target).forEach(member -> {
                                if (!member.equals(username)) {
                                    Server.startCall(member, this);
                                }
                            });
                        } else {
                            sendMessage("Usuario o grupo " + target + " no encontrado.");
                        }
                    }
                //para recibir la llamada
                } else if (message.startsWith("Llamada entrante")) {

                    String[] splitMessage = message.split(" ", 4);
                    if (splitMessage.length == 4) {
                        String caller = splitMessage[2];
                        String response = splitMessage[3];
                        if (response.equals("s")) {
                            sendMessage("Llamada aceptada. Conectando...");

                            Server.getClients().get(caller).sendMessage("Llamada aceptada. Conectando...");

                            Server.answerCall(username, caller);

                        } else {
                            sendMessage("Llamada rechazada.");
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

    static {
        try {
            callManager = new CallManager(40000);
        } catch (SocketException e) {
            throw new RuntimeException("Failed to initialize CallManager", e);
        }
    }
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.audioManager = new AudioManager(45000 + new Random().nextInt(1000));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    
    // Nuevo método para manejar llamadas
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

}
