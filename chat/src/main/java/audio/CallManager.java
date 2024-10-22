package audio;

import java.net.*;
import java.util.*;

public class CallManager {
    private final Map<String, Call> activeCalls = new HashMap<>();
    private final AudioManager audioManager;
    
    public CallManager(int basePort) throws SocketException {
        this.audioManager = new AudioManager(basePort);
    }
    
    public void startCall(String callId, String initiator, Set<String> participants, Map<String, InetAddress> addressMap) {
        Call call = new Call(callId, initiator, participants);
        activeCalls.put(callId, call);
        
        // Iniciar transmisión de audio para cada participante
        for (String participant : participants) {
            InetAddress participantAddress = addressMap.get(participant);
            if (participantAddress != null) {
                audioManager.startPlaying();
                // Asignar puertos únicos para cada participante
                int participantPort = call.assignPort(participant);
                try {
                    audioManager.startRecording(participantAddress, participantPort);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void endCall(String callId) {
        Call call = activeCalls.remove(callId);
        if (call != null) {
            audioManager.stopRecording();
            audioManager.stopPlaying();
        }
    }
    
    private static class Call {
        private final String callId;
        private final String initiator;
        private final Set<String> participants;
        private final Map<String, Integer> participantPorts = new HashMap<>();
        private static final int BASE_PORT = 50000;
        private static int nextPort = BASE_PORT;
        
        public Call(String callId, String initiator, Set<String> participants) {
            this.callId = callId;
            this.initiator = initiator;
            this.participants = new HashSet<>(participants);
        }
        
        public int assignPort(String participant) {
            return participantPorts.computeIfAbsent(participant, k -> nextPort++);
        }
    }
}