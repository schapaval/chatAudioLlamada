import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error configurando streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            out.println("Bienvenido al servidor! Por favor, ingresa tu nombre de usuario:");
            String username = in.readLine();  // Leer el nombre de usuario del cliente
            out.println("Hola " + username + "! Puedes empezar a escribir tus mensajes.");

            String message;
            // Leer mensajes enviados por el cliente
            while ((message = in.readLine()) != null) {
                System.out.println(username + ": " + message);  // Imprime el mensaje en el servidor
            }
        } catch (IOException e) {
            System.err.println("Error en la conexión con el cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando el socket del cliente: " + e.getMessage());
            }
        }
    }
}
