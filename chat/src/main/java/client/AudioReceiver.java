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
        this.clientSocket = new DatagramSocket();
        setupPlayer();
    }

    private void setupPlayer() throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
        playerThread = new PlayerThread(audioFormat, bufferSize);
        playerThread.start();
    }

    public void startReceiving() throws Exception {
        byte[] buffer = new byte[bufferSize];
        int count = 0;

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
                playerThread.addBytes(data);
                System.out.println("Recibido paquete " + packetCount + " actual: " + count);
            }
            count++;
        }
        clientSocket.close();
    }
}
