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
            String response = in.readLine();
            System.out.println("Respuesta del servidor: " + response);

            boolean exit = false;
            while (!exit) {
                System.out.println("\nMenú:");
                System.out.println("1. Crear grupo de chat");
                System.out.println("2. Enviar mensaje de texto");
                System.out.println("3. Enviar nota de voz");
                System.out.println("4. Realizar llamada");
                System.out.println("5. Salir");

                System.out.print("Selecciona una opción: ");
                int option = Integer.parseInt(scanner.nextLine());

                switch (option) {
                    case 1:
                        System.out.println("Función no implementada: Crear grupo de chat");
                        break;
                    case 2:
                        System.out.print("Escribe tu mensaje de texto: ");
                        String message = scanner.nextLine();
                        out.println("Texto: " + message);
                        response = in.readLine();
                        System.out.println("Respuesta del servidor: " + response);
                        guardarHistorial("Texto: " + message);
                        break;
                    case 3:
                        System.out.println("Simulando envío de nota de voz...");
                        out.println("Nota de voz enviada");
                        response = in.readLine();
                        System.out.println("Respuesta del servidor: " + response);
                        guardarHistorial("Nota de voz enviada");
                        break;
                    case 4:
                        System.out.println("Simulando llamada...");
                        out.println("Llamada realizada");
                        response = in.readLine();
                        System.out.println("Respuesta del servidor: " + response);
                        guardarHistorial("Llamada realizada");
                        break;
                    case 5:
                        exit = true;
                        System.out.println("Saliendo...");
                        break;
                    default:
                        System.out.println("Opción no válida.");
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
