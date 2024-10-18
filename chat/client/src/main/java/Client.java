import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";  // Dirección del servidor (localhost)
    private static final int SERVER_PORT = 12345;  // Puerto del servidor

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            // Leer el mensaje de bienvenida del servidor
            System.out.println(in.readLine());

            // Enviar el nombre de usuario al servidor
            System.out.print("Ingresa tu nombre de usuario: ");
            String username = userInput.readLine();
            out.println(username);

            // Leer y enviar mensajes al servidor
            String message;
            while ((message = userInput.readLine()) != null) {
                out.println(message);  // Enviar mensaje al servidor
            }

        } catch (IOException e) {
            System.err.println("Error en la conexión con el servidor: " + e.getMessage());
        }
    }
}
