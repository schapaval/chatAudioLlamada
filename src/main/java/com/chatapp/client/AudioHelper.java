package com.chatapp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class AudioHelper {

    private final AudioFormat audioFormat;
    private final TargetDataLine microphone;

    public AudioHelper() throws LineUnavailableException {
        // Define un formato de audio para la captura (frecuencia de muestreo, tamaño de muestra, etc.)
        audioFormat = new AudioFormat(44100, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
    }

    public byte[] captureAudio(int durationInSeconds) throws LineUnavailableException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (TargetDataLine mic = microphone) {
            mic.open(audioFormat);
            mic.start();

            byte[] buffer = new byte[1024];
            long end = System.currentTimeMillis() + (durationInSeconds * 1000);
            while (System.currentTimeMillis() < end) {
                int bytesRead = mic.read(buffer, 0, buffer.length);
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            mic.stop();
        }

        return byteArrayOutputStream.toByteArray();
    }
}
