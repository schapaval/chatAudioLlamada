package server;

import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        int port = 12345; // Puerto en el que el servidor escuchará
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor escuchando en el puerto " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Acepta la conexión del cliente
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                // Manejar la comunicación con el cliente
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            out.println("Bienvenido al servidor de chat");
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Mensaje del cliente: " + message);
                if (message.startsWith("Texto: ")) {
                    out.println("Mensaje de texto recibido");
                } else if (message.startsWith("Nota de voz")) {
                    out.println("Nota de voz recibida");
                } else if (message.startsWith("Llamada")) {
                    out.println("Llamada realizada correctamente");
                } else {
                    out.println("Comando no reconocido");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
