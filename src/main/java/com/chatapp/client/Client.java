package com.chatapp.client;

import java.util.Arrays;
import java.util.Scanner;

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
                    case 2:
                        System.out.print("Callee: ");
                        String callee = scanner.nextLine().trim();
                        chat.makeCall(username, callee);
                        break;
                    case 3:
                        System.out.print("Group Name: ");
                        String groupName = scanner.nextLine().trim();
                        System.out.print("Members (comma-separated): ");
                        String[] memberArray = scanner.nextLine().trim().split(",");
                        for (int i = 0; i < memberArray.length; i++) {
                            memberArray[i] = memberArray[i].trim();
                        }
                        StringSeq members = new StringSeq(Arrays.asList(memberArray));
                        chat.createGroup(groupName, members);
                        break;
                    case 4:
                        System.out.print("Recipient: ");
                        String voiceRecipient = scanner.nextLine().trim();
                        // For demonstration, we'll use an empty ByteSeq
                        ByteSeq voiceData = new ByteSeq(); // In a real application, you would capture audio data
                        chat.sendVoiceNote(username, voiceRecipient, voiceData);
                        break;
                    case 5:
                        System.out.println("Exiting...");
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
