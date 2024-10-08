// Server.java
package com.chatapp.server;

import ChatApp._ChatDisp;
import ChatApp.ChatException;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Util;

public class Server extends com.zeroc.Ice.Application {

    public static void main(String[] args) {
        Server app = new Server();
        int status = app.main("Server", args, "config.server");
        System.exit(status);
    }

    @Override
    public int run(String[] args) {
        com.zeroc.Ice.ObjectAdapter adapter = communicator().createObjectAdapter("ChatAdapter");
        com.zeroc.Ice.Object chatServant = new ChatI();
        adapter.add(chatServant, Util.stringToIdentity("ChatService"));
        adapter.activate();
        communicator().waitForShutdown();
        return 0;
    }

    private class ChatI extends _ChatDisp {
        @Override
        public void sendMessage(String sender, String recipient, String content, Current current) throws ChatException {
            System.out.println(sender + " sent message to " + recipient + ": " + content);
        }

        @Override
        public void makeCall(String caller, String callee, Current current) throws ChatException {
            System.out.println(caller + " is calling " + callee);
        }

        @Override
        public void createGroup(String groupName, String[] members, Current current) throws ChatException {
            System.out.println("Group " + groupName + " created with members:");
            for (String member : members) {
                System.out.println(" - " + member);
            }
        }

        @Override
        public void sendVoiceNote(String sender, String recipient, byte[] voiceData, Current current) throws ChatException {
            System.out.println(sender + " sent a voice note to " + recipient);
        }
    }
}
