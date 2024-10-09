package com.chatapp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class AudioHelper {

    private AudioFormat audioFormat;
    private TargetDataLine microphone;

    public AudioHelper() throws LineUnavailableException {
        // Define un formato de audio para la captura (frecuencia de muestreo, tamaño de muestra, etc.)
        audioFormat = new AudioFormat(44100, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
    }

    public byte[] captureAudio(int durationInSeconds) throws LineUnavailableException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        microphone.open(audioFormat);
        microphone.start();

        byte[] buffer = new byte[1024];
        long end = System.currentTimeMillis() + (durationInSeconds * 1000);
        while (System.currentTimeMillis() < end) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        microphone.stop();
        microphone.close();

        return byteArrayOutputStream.toByteArray();
    }
}
