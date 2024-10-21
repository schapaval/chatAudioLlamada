package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String host = "localhost"; // Cambia a la IP del servidor si es remoto
        int port = 12345; // Puerto al que el cliente se conectará
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Conectado al servidor en " + host + ":" + port);

            // Leer el mensaje del servidor para ingresar el username
            String serverPrompt = in.readLine();
            System.out.println("Respuesta del servidor: " + serverPrompt);

            // Enviar el username al servidor
            String username = scanner.nextLine();
            out.println(username);

            // Esperar la bienvenida del servidor
            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            // Iniciar el hilo para leer mensajes del servidor
            new Thread(new ReadMessages(in)).start();

            // Hilo principal para enviar mensajes al servidor
            while (true) {
                String message = scanner.nextLine();
                out.println(message);  // Envía el mensaje al servidor

                // Guardar en el historial de mensajes enviados
                guardarHistorial(message);
                
                if (message.equalsIgnoreCase("salir")) {
                    System.out.println("Desconectando del servidor...");
                    break;  // Sale del ciclo cuando el cliente escribe "salir"
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para guardar el historial de mensajes
    private static void guardarHistorial(String mensaje) {
        try (FileWriter fw = new FileWriter("historial.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(mensaje);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ReadMessages implements Runnable {
    private BufferedReader in;

    public ReadMessages(BufferedReader in) {
        this.in = in;
    }

    @Override
    public void run() {
        String message;
        try {
            // Mientras recibe mensajes, los imprime en consola
            while ((message = in.readLine()) != null) {
                System.out.println("\nMensaje recibido del servidor: " + message);
            }
        } catch (IOException e) {
            System.out.println("Conexión cerrada con el servidor.");
        }
    }
}
