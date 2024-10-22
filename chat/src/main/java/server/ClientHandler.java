package server;

import java.io.*;
import java.net.*;
import java.util.*;

import audio.AudioManager;
import audio.AudioStreamManager;
import audio.Call;
import audio.CallManager;
import audio.AudioManager;
import audio.CallManager;

public class ClientHandler extends Thread {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private AudioStreamManager audioStream;
    private Call currentCall;
    private static final int BASE_AUDIO_PORT = 50000;
    /**
   
     
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
*/

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.audioStream = new AudioStreamManager(BASE_AUDIO_PORT + new Random().nextInt(1000));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para manejar comandos de llamada
    private void handleCallCommand(String message) {
        String[] parts = message.split(" ", 2);
        if (parts.length != 2) return;

        String target = parts[1];
        if (target.startsWith("grupo:")) {
            initiateGroupCall(target.substring(6));
        } else {
            initiatePrivateCall(target);
        }
    }

    private void initiatePrivateCall(String targetUser) {
        ClientHandler targetHandler = Server.getClients().get(targetUser);
        if (targetHandler == null) {
            sendMessage("Usuario no encontrado: " + targetUser);
            return;
        }

        String callId = UUID.randomUUID().toString();
        Set<String> participants = new HashSet<>(Arrays.asList(username, targetUser));
        Call call = new Call(callId, username, participants, false, null);
        
        // Notificar al usuario objetivo
        targetHandler.sendMessage("CALL_REQUEST|" + callId + "|" + username);
        currentCall = call;
        Server.registerCall(call);
        
        sendMessage("Esperando que " + targetUser + " acepte la llamada...");
    }

    private void initiateGroupCall(String groupName) {
        Set<String> groupMembers = Server.getGroups().get(groupName);
        if (groupMembers == null) {
            sendMessage("Grupo no encontrado: " + groupName);
            return;
        }

        String callId = "group-" + UUID.randomUUID().toString();
        Call call = new Call(callId, username, groupMembers, true, groupName);
        
        // Notificar a todos los miembros del grupo
        for (String member : groupMembers) {
            if (!member.equals(username)) {
                ClientHandler memberHandler = Server.getClients().get(member);
                if (memberHandler != null) {
                    memberHandler.sendMessage("GROUP_CALL_REQUEST|" + callId + "|" + username + "|" + groupName);
                }
            }
        }
        
        currentCall = call;
        Server.registerCall(call);
        sendMessage("Esperando que los miembros del grupo acepten la llamada...");
    }

    private void handleCallResponse(String callId, boolean accept) {
        Call call = Server.getCall(callId);
        if (call == null) return;

        if (accept) {
            call.acceptCall(username);
            if (call.allParticipantsAccepted()) {
                startCallAudio(call);
            }
        } else {
            call.declineCall(username);
            notifyCallDeclined(call);
        }
    }

    private void startCallAudio(Call call) {
        try {
            InetAddress targetAddress = socket.getInetAddress();
            int targetPort = BASE_AUDIO_PORT + call.getParticipants().size();
            audioStream.startStreaming(targetAddress, targetPort);
            
            // Notificar a todos los participantes
            for (String participant : call.getParticipants()) {
                ClientHandler handler = Server.getClients().get(participant);
                if (handler != null) {
                    handler.sendMessage("CALL_STARTED|" + call.getCallId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyCallDeclined(Call call) {
        for (String participant : call.getParticipants()) {
            ClientHandler handler = Server.getClients().get(participant);
            if (handler != null) {
                handler.sendMessage("CALL_DECLINED|" + call.getCallId() + "|" + username);
            }
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
                    handleCallCommand(message);
                } else if (message.startsWith("CALL_ACCEPT|")) {
                    String callId = message.split("\\|")[1];
                    handleCallResponse(callId, true);
                } else if (message.startsWith("CALL_DECLINE|")) {
                    String callId = message.split("\\|")[1];
                    handleCallResponse(callId, false);
                } else if (message.equals("/colgar")) {
                    if (currentCall != null) {
                        audioStream.stopStreaming();
                        Server.endCall(currentCall.getCallId());
                        currentCall = null;
                    }
                }
                else if (message.equals("salir")) {
                    break;
                } else {
                    // Enviar mensaje público
                    Server.broadcast(message, this);
                }

                
                
                Server.saveMessageHistory(username, message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (audioStream != null) {
                audioStream.stopStreaming();
            }
            try {
                socket.close();
                Server.removeClient(username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //_____________________________Rescatados______________
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
}


