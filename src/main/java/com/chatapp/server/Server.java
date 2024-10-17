package com.chatapp.server;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.zeroc.Ice.Object;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class Server extends com.zeroc.Ice.Application {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args)) {
            System.out.println("Configuración del servidor cargada.");

            // Asegúrate de que el nombre del adaptador coincida con el del archivo de
            // configuración
            com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapter("ChatAdapter");

            System.out.println("Adapter creado, escuchando en puerto...");
            adapter.add(new ChatI(), com.zeroc.Ice.Util.stringToIdentity("Chat"));
            adapter.activate();

            System.out.println("Servidor activado.");
            communicator.waitForShutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int run(String[] args) {
        ObjectAdapter adapter = null;
        try {
            // Crea y activa el adaptador desde los parámetros de configuración
            adapter = communicator().createObjectAdapter("ChatAdapter");
            Object chatServant = new ChatI();
            adapter.add(chatServant, Util.stringToIdentity("ChatService"));
            adapter.activate();

            System.out.println("Servidor en ejecución, esperando conexiones...");
            communicator().waitForShutdown();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception in run: {0}", e.getMessage());
            return 1;
        } finally {
            if (adapter != null) {
                adapter.destroy();
            }
        }
        return 0;
    }
}
