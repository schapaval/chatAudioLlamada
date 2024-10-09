module ChatApp {
    // Define sequences
    sequence<string> StringSeq;
    sequence<byte> ByteSeq;

    // Define exception
    exception ChatException {
        string reason;
    }

    // Define interface
    interface Chat {
        void sendMessage(string sender, string recipient, string content);
        void makeCall(string caller, string callee);
        void createGroup(string groupName, StringSeq members);
        void sendVoiceNote(string sender, string recipient, ByteSeq voiceData);
    }
}
