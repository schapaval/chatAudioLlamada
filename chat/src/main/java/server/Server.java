package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    // Almacenamos los usernames y los sockets de los clientes conectados
    private static Map<String, Socket> clients = new HashMap<>();

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

    static class ClientHandler extends Thread {
        private Socket clientSocket;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                // Solicitar el username al cliente
                out.println("Ingresa tu username:");
                username = in.readLine();
                synchronized (clients) {
                    clients.put(username, clientSocket); // Guardar el username y el socket
                }
                out.println("Bienvenido " + username + ", estás conectado al servidor de chat");
                System.out.println(""+username+ " se ha conectado");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Texto privado: ")) {
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3) {
                            String targetUsername = parts[1].trim();
                            String privateMessage = parts[2].trim();
                            enviarMensajePrivado(targetUsername, privateMessage);
                        } else {
                            out.println("Formato incorrecto. Usa: Texto privado: [username]: [mensaje]");
                        }
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
                    synchronized (clients) {
                        clients.remove(username); // Eliminar al cliente desconectado
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void enviarMensajePrivado(String targetUsername, String message) {
            synchronized (clients) {
                Socket targetSocket = clients.get(targetUsername);
                if (targetSocket != null) {
                    try {
                        PrintWriter out = new PrintWriter(targetSocket.getOutputStream(), true);
                        out.println("Mensaje privado de " + username + ": " + message);
                        System.out.println("Mensaje privado de " + username + " a " + targetUsername + ": " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    PrintWriter out;
                    try {
                        out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println("Usuario " + targetUsername + " no está conectado.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
