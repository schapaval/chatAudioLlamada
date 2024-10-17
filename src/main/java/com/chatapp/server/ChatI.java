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
    private final Map<String, Set<String>> groups = new HashMap<>();
    private final Map<String, Boolean> activeCalls = new HashMap<>(); // Para gestionar llamadas activas

    @SuppressWarnings("unchecked")
    public void createGroup(String groupName, StringSeq members) {
        Set<String> memberSet = new HashSet<>();
        memberSet.addAll((Collection<? extends String>) members);
        groups.put(groupName, memberSet);
        System.out.println("Group " + groupName + " created with members: " + members);
    }

    public void sendMessageToGroup(String sender, String groupName, String message) {
        if (groups.containsKey(groupName)) {
            Set<String> groupMembers = groups.get(groupName);
            for (String member : groupMembers) {
                if (!member.equals(sender)) {
                    System.out.println("Message from " + sender + " to " + member + ": " + message);
                }
            }
        } else {
            System.out.println("Group " + groupName + " does not exist.");
        }
    }

    public void sendVoiceNoteToGroup(String sender, String groupName, ByteSeq voiceData) {
        if (groups.containsKey(groupName)) {
            Set<String> groupMembers = groups.get(groupName);
            for (String member : groupMembers) {
                if (!member.equals(sender)) {
                    System.out.println("Voice note from " + sender + " to " + member);
                }
            }
        } else {
            System.out.println("Group " + groupName + " does not exist.");
        }
    }

    public void sendMessage(String sender, String recipient, String message) {
        System.out.println("Message from " + sender + " to " + recipient + ": " + message);
    }

    public void sendVoiceNote(String sender, String recipient, ByteSeq voiceData) {
        System.out.println("Voice note from " + sender + " to " + recipient);
    }

    public void makeCall(String caller, String callee) {
        if (activeCalls.getOrDefault(caller, false)) {
            System.out.println("Caller " + caller + " is already on a call.");
        } else {
            activeCalls.put(caller, true);
            activeCalls.put(callee, true);
            System.out.println("Call initiated from " + caller + " to " + callee);
        }
    }

    public void endCall(String caller, String callee) {
        activeCalls.put(caller, false);
        activeCalls.put(callee, false);
        System.out.println("Call ended between " + caller + " and " + callee);
    }

    // Agregar un miembro a un grupo existente
    public void addMemberToGroup(String groupName, String newMember) {
        if (groups.containsKey(groupName)) {
            Set<String> groupMembers = groups.get(groupName);
            if (!groupMembers.contains(newMember)) {
                groupMembers.add(newMember);
                System.out.println(newMember + " added to group " + groupName);
            } else {
                System.out.println(newMember + " is already a member of the group.");
            }
        } else {
            System.out.println("Group " + groupName + " does not exist.");
        }
    }

    // Eliminar un miembro de un grupo
    public void removeMemberFromGroup(String groupName, String member) {
        if (groups.containsKey(groupName)) {
            Set<String> groupMembers = groups.get(groupName);
            if (groupMembers.remove(member)) {
                System.out.println(member + " removed from group " + groupName);
            } else {
                System.out.println(member + " is not a member of the group.");
            }
        } else {
            System.out.println("Group " + groupName + " does not exist.");
        }
    }

    // Obtener la lista de miembros de un grupo
    public void listGroupMembers(String groupName) {
        if (groups.containsKey(groupName)) {
            Set<String> groupMembers = groups.get(groupName);
            System.out.println("Members of group " + groupName + ": " + groupMembers);
        } else {
            System.out.println("Group " + groupName + " does not exist.");
        }
    }
}
