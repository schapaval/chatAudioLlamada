// Archivo: ReadMessages.java
package client;

import java.io.BufferedReader;
import java.io.IOException;

public class ReadMessages implements Runnable {
    private BufferedReader in;

    public ReadMessages(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                System.out.println("\nMensaje recibido: " + message);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo mensajes del servidor.");
        }
    }
}
