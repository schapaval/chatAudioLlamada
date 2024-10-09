package com.chatapp.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zeroc.Ice.Object;

import ChatApp.ByteSeq;
import ChatApp.StringSeq;

public class ChatI implements Object {
    // Mapa para gestionar grupos y sus miembros
    private final Map<String, Set<String>> groups = new HashMap<>();

    // Método para crear un grupo de chat
    @SuppressWarnings("unchecked")
    public void createGroup(String groupName, StringSeq members) {
        Set<String> memberSet = new HashSet<>();
        memberSet.addAll((Collection<? extends String>) members);
        groups.put(groupName, memberSet); // Almacena el grupo y sus miembros
        System.out.println("Group " + groupName + " created with members: " + members);
    }

    // Método para enviar mensajes a los miembros de un grupo (excluyendo al remitente)
    public void sendMessageToGroup(String sender, String groupName, String message) {
        if (groups.containsKey(groupName)) {
            Set<String> groupMembers = groups.get(groupName);
            for (String member : groupMembers) {
                if (!member.equals(sender)) {
                    // Aquí iría la lógica para enviar un mensaje a cada miembro (simulación)
                    System.out.println("Message from " + sender + " to " + member + ": " + message);
                }
            }
        } else {
            System.out.println("Group " + groupName + " does not exist.");
        }
    }

    // Método para enviar notas de voz a los miembros de un grupo (excluyendo al remitente)
    public void sendVoiceNoteToGroup(String sender, String groupName, ByteSeq voiceData) {
        if (groups.containsKey(groupName)) {
            Set<String> groupMembers = groups.get(groupName);
            for (String member : groupMembers) {
                if (!member.equals(sender)) {
                    // Aquí iría la lógica para enviar la nota de voz a cada miembro (simulación)
                    System.out.println("Voice note from " + sender + " to " + member);
                }
            }
        } else {
            System.out.println("Group " + groupName + " does not exist.");
        }
    }

    // Método para manejar el envío de un mensaje directo de un usuario a otro
    public void sendMessage(String sender, String recipient, String message) {
        // Aquí iría la lógica para enviar el mensaje a un destinatario específico (simulación)
        System.out.println("Message from " + sender + " to " + recipient + ": " + message);
    }

    // Método para manejar el envío de notas de voz directas de un usuario a otro
    public void sendVoiceNote(String sender, String recipient, ByteSeq voiceData) {
        // Aquí iría la lógica para enviar la nota de voz a un destinatario específico (simulación)
        System.out.println("Voice note from " + sender + " to " + recipient);
    }

    // Método para manejar las llamadas de voz entre usuarios
    public void makeCall(String caller, String callee) {
        // Aquí iría la lógica para iniciar una llamada entre dos usuarios (simulación)
        System.out.println("Call initiated from " + caller + " to " + callee);
    }
}
