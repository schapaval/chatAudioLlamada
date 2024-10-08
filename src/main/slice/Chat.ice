// Chat.ice
module ChatApp {
    interface Chat {
        void sendMessage(string sender, string recipient, string content);
        void makeCall(string caller, string callee);
        void createGroup(string groupName, string[] members);
        void sendVoiceNote(string sender, string recipient, bytes voiceData);
    }

    exception ChatException {
        string reason;
    }
}
