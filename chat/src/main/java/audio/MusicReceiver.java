package audio;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import javax.sound.sampled.*;

public class MusicReceiver {
    public static void main(String[] args) throws Exception {
        InetAddress IPAddress = InetAddress.getByName("localhost");
        int PORT = 6789;
        int BUFFER_SIZE = 1024 + 4;
        DatagramSocket clientSocket = new DatagramSocket();
        PlayerThread playerThread;

        // Configurar el formato de audio y el hilo de reproducción
        AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
        playerThread = new PlayerThread(audioFormat, BUFFER_SIZE);
        playerThread.start();

        // Enviar solicitud al servidor para iniciar la transmisión
        String mensaje = "Hola servidor, enviame una cancion... #";
        byte[] sendData = mensaje.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, PORT);
        clientSocket.send(sendPacket);

        byte[] buffer = new byte[BUFFER_SIZE];
        int count = 0;

        // Recibir los paquetes y reproducir el audio en tiempo real
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            clientSocket.receive(packet);
            buffer = packet.getData();
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            int packetCount = byteBuffer.getInt();

            if (packetCount == -1) {
                System.out.println("Recibido último paquete " + count);
                break;
            } else {
                byte[] data = new byte[1024];
                byteBuffer.get(data, 0, data.length);
                playerThread.addBytes(data);  // Agregar datos al hilo de reproducción

                System.out.println("Recibido paquete " + packetCount + " actual: " + count);
            }
            count++;
        }
        clientSocket.close();
    }
}

