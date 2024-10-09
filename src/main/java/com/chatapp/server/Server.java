package com.chatapp.server;

import java.util.logging.Logger;

import com.zeroc.Ice.Object;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

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
            Object chatServant = (Object) new ChatI();
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