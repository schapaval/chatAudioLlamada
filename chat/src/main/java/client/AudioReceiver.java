// Archivo: AudioReceiver.java
package client;

import java.io.*;
import java.net.*;
import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import audio.PlayerThread;

public class AudioReceiver {

    private DatagramSocket clientSocket;
    private int bufferSize = 1024 + 4;
    private InetAddress serverIPAddress;
    private int port;
    private PlayerThread playerThread;

    public AudioReceiver(InetAddress serverIPAddress, int port) throws Exception {
        this.serverIPAddress = serverIPAddress;
        this.port = port;
        this.clientSocket = new DatagramSocket(port); // Asociar socket al puerto
        setupPlayer();
    }

    // Configura el reproductor con el formato de audio
    private void setupPlayer() throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
        playerThread = new PlayerThread(audioFormat, bufferSize);
        playerThread.start();
    }

    // Comienza a recibir audio en tiempo real
    public void startReceiving() throws Exception {
        byte[] buffer = new byte[bufferSize];
        int count = 0;

        System.out.println("Esperando audio en el puerto " + port + "...");

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            clientSocket.receive(packet); // Recibir paquete UDP
            buffer = packet.getData(); // Extraer datos del paquete
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            int packetCount = byteBuffer.getInt(); // Leer número de paquete

            if (packetCount == -1) { // Indica el último paquete
                System.out.println("Recibido último paquete: " + count);
                break;
            } else {
                byte[] data = new byte[1024];
                byteBuffer.get(data, 0, data.length); // Extraer datos del paquete
                playerThread.addBytes(data); // Enviar datos al reproductor
                System.out.println("Recibido paquete " + packetCount + " actual: " + count);
            }
            count++;
        }

        closeSocket(); // Cerrar el socket al finalizar
    }

    // Cierra el socket
    private void closeSocket() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
            System.out.println("Socket cerrado.");
        }
    }
}
