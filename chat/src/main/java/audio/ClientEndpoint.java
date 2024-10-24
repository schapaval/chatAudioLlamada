package audio;

import java.net.*;

public class ClientEndpoint {
    private final InetAddress address;
    private final int port;
    private final AudioManager audioManager;

    public ClientEndpoint(InetAddress address, int port) throws SocketException {
        this.address = address;
        this.port = port;
        this.audioManager = new AudioManager(port);
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }
}
