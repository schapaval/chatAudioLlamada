package com.chatapp.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import ChatApp.ChatPrx;
import ChatApp.ChatPrxHelper;

public class Client {

    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            ChatPrx chat = ChatPrxHelper.checkedCast(
                communicator.stringToProxy("ChatService:default -p 10000")
            );
            if (chat == null) {
                throw new Error("Invalid proxy");
            }

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your username: ");
            String username = scanner.nextLine().trim();

            while (true) {
                System.out.println("\nCommands:");
                System.out.println("1. Send Message");
                System.out.println("2. Make Call");
                System.out.println("3. Create Group");
                System.out.println("4. Send Voice Note");
                System.out.println("5. Exit");
                System.out.print("Select an option: ");

                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number between 1 and 5.");
                    continue;
                }

                switch (choice) {
                    case 1:
                        System.out.print("Recipient: ");
                        String recipient = scanner.nextLine().trim();
                        System.out.print("Message: ");
                        String message = scanner.nextLine().trim();
                        chat.sendMessage(username, recipient, message);
                        break;

                    case 2: // Voice Call
                        System.out.print("Callee: ");
                        String callee = scanner.nextLine().trim();

                        AudioHelper callAudioHelper = new AudioHelper();
                        Thread sendAudioThread = new Thread(() -> {
                            try {
                                while (true) {
                                    byte[] audioData = callAudioHelper.captureAudio(1); // Capture 1 second of audio
                                    chat.sendVoiceCall(username, callee, audioData); // Send audio data directly
                                }
                            } catch (IOException | LineUnavailableException e) {
                                e.printStackTrace();
                            }
                        });

                        sendAudioThread.start();
                        break;

                    case 3:
                        System.out.print("Group Name: ");
                        String groupName = scanner.nextLine().trim();
                        System.out.print("Members (comma-separated): ");
                        String[] memberArray = scanner.nextLine().trim().split(",");
                        for (int i = 0; i < memberArray.length; i++) {
                            memberArray[i] = memberArray[i].trim();
                        }
                        chat.createGroup(groupName, memberArray); // Use String[]
                        break;

                    case 4:
                        System.out.print("Recipient: ");
                        String voiceRecipient = scanner.nextLine().trim();

                        // Capture 5 seconds of audio
                        AudioHelper audioHelper = new AudioHelper();
                        byte[] audioData = audioHelper.captureAudio(5);

                        chat.sendVoiceNote(username, voiceRecipient, audioData); // Send byte[]
                        break;

                    case 5:
                        System.out.println("Exiting...");
                        scanner.close();
                        return;

                    default:
                        System.out.println("Invalid option. Please select a number between 1 and 5.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
