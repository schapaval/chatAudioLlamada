package com.chatapp.server;

import ChatApp._ChatDisp;
import ChatApp.ChatException;
import ChatApp.StringSeqHelper;
import ChatApp.ByteSeqHelper;

import com.zeroc.Ice.Current;
import com.zeroc.Ice.Util;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Object;
import java.util.logging.Logger;

public class Server extends com.zeroc.Ice.Application {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        Server app = new Server();
        int status = 0;
        try {
            status = app.main("Server", args, "config.server");
        } catch (Exception e) {
            logger.severe("Exception in main: " + e.getMessage());
            status = 1;
        }
        System.exit(status);
    }

    @Override
    public int run(String[] args) {
        ObjectAdapter adapter = null;
        try {
            adapter = communicator().createObjectAdapter("ChatAdapter");
            Object chatServant = new ChatI();
            adapter.add(chatServant, Util.stringToIdentity("ChatService"));
            adapter.activate();
            communicator().waitForShutdown();
        } catch (Exception e) {
            logger.severe("Exception in run: " + e.getMessage());
            return 1;
        } finally {
            if (adapter != null) {
                adapter.destroy();
            }
        }
        return 0;
    }
}