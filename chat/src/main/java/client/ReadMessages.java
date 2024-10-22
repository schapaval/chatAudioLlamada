package client;

import java.io.BufferedReader;
import java.io.IOException;

public class ReadMessages implements Runnable {
    private BufferedReader in;
    private Client client;

    public ReadMessages(BufferedReader in, Client client) {
        this.in = in;
        this.client = client;
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/llamada_entrante")) {
                    String caller = message.split(" ", 2)[1];
                    client.handleIncomingCall(caller);
                } else if (message.equals("/llamada_aceptada")) {
                    System.out.println("Llamada aceptada. Iniciando...");
                    client.setInCall(true);
                    client.startCall();
                } else if (message.equals("/llamada_rechazada")) {
                    System.out.println("Llamada rechazada.");
                } else if (message.equals("/llamada_finalizada")) {
                    System.out.println("El otro usuario ha finalizado la llamada.");
                    client.endCall();
                } else {
                    System.out.println("\nMensaje recibido: " + message);
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo mensajes del servidor.");
        }
    }
}