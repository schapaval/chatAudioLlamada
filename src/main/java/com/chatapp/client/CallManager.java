package com.chatapp.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class CallManager {

    private AudioFormat audioFormat;

    public CallManager() {
        audioFormat = new AudioFormat(44100, 16, 1, true, true);
    }

    public void playAudio(byte[] audioData) throws LineUnavailableException, IOException {
        SourceDataLine speakers = AudioSystem.getSourceDataLine(audioFormat);
        speakers.open(audioFormat);
        speakers.start();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = byteArrayInputStream.read(buffer)) != -1) {
            speakers.write(buffer, 0, bytesRead);
        }

        speakers.drain();
        speakers.close();
    }
}
