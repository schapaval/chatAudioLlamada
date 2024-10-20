package client;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String host = "localhost"; // Cambia a la IP del servidor si es remoto
        int port = 12345; // Puerto al que el cliente se conectará

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Conectado al servidor en " + host + ":" + port);
            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            // Envía un mensaje al servidor
            out.println("Hola desde el cliente");
            response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
