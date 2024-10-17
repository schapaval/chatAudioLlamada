module ChatApp {
    sequence<string> StringSeq;
    sequence<byte> ByteSeq;

    exception ChatException {
        string reason;
    }

    interface Chat {
        void sendMessage(string sender, string recipient, string message);
        void sendVoiceCall(string username, string callee, ByteSeq callVoiceData);
        void createGroup(string groupName, StringSeq members);
        void sendVoiceNote(string username, string recipient, ByteSeq voiceData);
    };
};
