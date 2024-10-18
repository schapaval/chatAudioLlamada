import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;  // Puerto del servidor
    private ExecutorService pool;

    public Server() {
        // Un pool de threads para manejar múltiples clientes
        pool = Executors.newFixedThreadPool(10);  // Maneja hasta 10 clientes simultáneamente
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);

            // Aceptar conexiones de clientes de manera infinita
            while (true) {
                Socket clientSocket = serverSocket.accept();  // Acepta la conexión del cliente
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
                
                // Delegar la conexión a un ClientHandler
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);  // Ejecuta el ClientHandler en un nuevo thread
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
