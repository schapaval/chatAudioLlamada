// CallManager.java
package audio;

import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CallManager {
    private final Map<String, Call> activeCalls = new ConcurrentHashMap<>();
    private final int basePort;

    public CallManager(int basePort) {
        this.basePort = basePort;
    }

    public void startCall(String callId, String initiator, Set<String> participants, Map<String, InetAddress> addressMap) {
        Call call = new Call(callId, initiator, participants, basePort);
        activeCalls.put(callId, call);

        // Create AudioManager for each participant
        for (String participant : participants) {
            try {
                AudioManager audioManager = new AudioManager(call.assignPort(participant));
                call.addAudioManager(participant, audioManager);

                // Start audio transmission for each participant
                InetAddress participantAddress = addressMap.get(participant);
                if (participantAddress != null) {
                    audioManager.startPlaying();
                    audioManager.startRecording(participantAddress, call.assignPort(participant));
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    public void endCall(String callId) {
        Call call = activeCalls.remove(callId);
        if (call != null) {
            call.end();
        }
    }

    private static class Call {
        private final String callId;
        private final String initiator;
        private final Set<String> participants;
        private final Map<String, Integer> participantPorts = new HashMap<>();
        private final Map<String, AudioManager> audioManagers = new HashMap<>();
        private final int basePort;

        public Call(String callId, String initiator, Set<String> participants, int basePort) {
            this.callId = callId;
            this.initiator = initiator;
            this.participants = new HashSet<>(participants);
            this.basePort = basePort;
        }

        public int assignPort(String participant) {
            return participantPorts.computeIfAbsent(participant,
                    k -> basePort + participants.size() + participantPorts.size());
        }

        public void addAudioManager(String participant, AudioManager manager) {
            audioManagers.put(participant, manager);
        }

        public void end() {
            for (AudioManager manager : audioManagers.values()) {
                manager.close();
            }
            audioManagers.clear();
        }
    }
}