package audio;

import java.net.*;
import java.util.*;

public class CallManager {
    private final Map<String, Call> activeCalls = new HashMap<>();
    private final int basePort;

    // Constructor que guarda el puerto base
    public CallManager(int basePort) {
        this.basePort = basePort;
    }

    // Inicia una llamada entre participantes
    public void startCall(String callId, Set<ClientEndpoint> participants) {
        Call call = new Call(callId, participants);
        activeCalls.put(callId, call);

        // Iniciar la transmisión de audio para cada participante
        participants.forEach(participant -> {
            new Thread(() -> {
                try {
                    participant.getAudioManager().startPlaying();
                    participant.getAudioManager().startRecording(participant.getAddress(), participant.getPort());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });

        System.out.println("Llamada iniciada: " + callId);
    }

    public void endCall(String callId) {
        Call call = activeCalls.remove(callId);
        if (call != null) {
            call.getParticipants().forEach(participant -> participant.getAudioManager().stopRecording());
            System.out.println("Llamada finalizada: " + callId);
        }
    }

    // Clase interna para manejar llamadas
    private static class Call {
        private final String callId;
        private final Set<ClientEndpoint> participants;

        public Call(String callId, Set<ClientEndpoint> participants) {
            this.callId = callId;
            this.participants = participants;
        }

        public Set<ClientEndpoint> getParticipants() {
            return participants;
        }
    }

    public void startCall(String callId, String username, Set<String> participants,
            Map<String, InetAddress> addressMap) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startCall'");
    }
}
