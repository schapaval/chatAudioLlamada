package audio;

import java.util.*;
import java.net.*;

public class Call {
    private final String callId;
    private final String initiator;
    private final Set<String> participants;
    private final Map<String, Boolean> acceptanceStatus;
    private CallState state;
    private final boolean isGroupCall;
    private final String groupName;

    public Call(String callId, String initiator, Set<String> participants, boolean isGroupCall, String groupName) {
        this.callId = callId;
        this.initiator = initiator;
        this.participants = participants;
        this.acceptanceStatus = new HashMap<>();
        this.state = CallState.WAITING;
        this.isGroupCall = isGroupCall;
        this.groupName = groupName;
        
        // Inicializar estado de aceptación
        for (String participant : participants) {
            if (!participant.equals(initiator)) {
                acceptanceStatus.put(participant, false);
            }
        }
    }

    public void acceptCall(String participant) {
        acceptanceStatus.put(participant, true);
        if (allParticipantsAccepted()) {
            state = CallState.ACTIVE;
        }
    }

    public void declineCall(String participant) {
        state = CallState.DECLINED;
    }

    public boolean allParticipantsAccepted() {
        return acceptanceStatus.values().stream().allMatch(accepted -> accepted);
    }

    // Getters
    public String getCallId() { return callId; }
    public String getInitiator() { return initiator; }
    public Set<String> getParticipants() { return participants; }
    public CallState getState() { return state; }
    public boolean isGroupCall() { return isGroupCall; }
    public String getGroupName() { return groupName; }
}
