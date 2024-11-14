/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.messenger;

/**
 *
 * @author joshokeeffe
 */
import java.io.*;
import java.net.*;
import java.util.Scanner;

import javax.crypto.SecretKey;

public class Client1 {

    private static InetAddress host;
    private static final int PORT = 1234;
    private static Socket connection;
    private static SecretKey secretKey;
    private static BufferedReader in;
    private static PrintWriter out;
    

    public static void main(String[] args) {
        try {
            host = InetAddress.getLocalHost();
            secretKey = Encrypt.getSecretKey();
            // Open a connection to the server
            connection = new Socket(host, PORT);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            out = new PrintWriter(connection.getOutputStream(), true);
            boolean login = false;

            System.out.println("\nConnected to server successfully.");

            // Create a scanner to get user input from the terminal
            Scanner scanner = new Scanner(System.in);

            // Loop to keep the program running until the user types "exit"
            while (!login) {
                // Ask the user to choose "login" or "register"
                System.out.println("\nChoose an action: 'login' or 'register' (type 'exit' to quit):");
                String action = scanner.nextLine().trim().toLowerCase();

                // If the user types "exit", exit the loop and end the program
                if ("exit".equals(action)) {
                    System.out.println("Exiting program...");
                    break;
                }

                // Check if the action is "login" or "register"
                if (!"login".equals(action) && !"register".equals(action)) {
                    System.out.println("Invalid action. Please choose 'login' or 'register'.");
                    continue;
                }

                // Ask the user to enter a username and password
                System.out.print("\nEnter username: ");
                String username = scanner.nextLine().trim();

                // Check if username is empty
                if (username.isEmpty()) {
                    System.out.println("Error: Username cannot be empty.");
                    continue; // Go back to ask for the action again
                }

                System.out.print("Enter password: ");
                String password = scanner.nextLine().trim();

                // Check if password is empty
                if (password.isEmpty()) {
                    System.out.println("Error: Password cannot be empty.");
                    continue; // Go back to ask for the action again
                }

                // If the action is "register", ask the user to confirm the password
                if ("register".equals(action)) {
                    System.out.print("Re-enter password: ");
                    String passwordRe = scanner.nextLine().trim();

                    // Check if the passwords match
                    if (!password.equals(passwordRe)) {
                        System.out.println("Error: Passwords do not match.");
                        continue;
                    }
                }

                // Send the action, username, and password to the server
                String result = performAction(action, username, password);
                System.out.println("Server response >>> " + result);
                if(result.equals("Login successful.")){
                    login = true;
                }
            }
        } catch (Exception e) {
            System.out.println("Error generating encryption key.");
            e.printStackTrace();
            System.exit(1);
        }
        run();
    }

    private static void run() {
        try (Socket link = new Socket(host, PORT);
                PrintWriter out = new PrintWriter(link.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(link.getInputStream()));
                BufferedReader userEntry = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println(in.readLine()); // Prompt for username
            String username = userEntry.readLine();
            out.println(username);
            System.out.println(in.readLine()); // Welcome message

            System.out.println(in.readLine()); // Prompt to enter recipient's username
            String targetUsername = userEntry.readLine();
            out.println(targetUsername);

            String response = in.readLine();
            if (response.startsWith("User")) {
                System.out.println(response);
                return;
            }
            System.out.println(response); // Chat start confirmation

            // Separate thread for receiving and decrypting messages
            new Thread(new Runnable() {
                public void run() {
                    try {
                        String serverMessage;
                        while ((serverMessage = in.readLine()) != null) {
                            // Clear the current line and print the incoming message
                            System.out.print("\r"); // Move the cursor to the start of the line
                            System.out.println(targetUsername + ": " + decryptMessage(serverMessage));

                            // Reprint the "You: " prompt after displaying the received message
                            System.out.print("\nYou: ");
                        }
                    } catch (IOException e) {
                        System.out.println("Connection lost.");
                    }
                }

                private String decryptMessage(String message) {
                    try {
                        return Encrypt.decrypt(message, secretKey);
                    } catch (Exception e) {
                        return "Decryption error.";
                    }
                }
            }).start();

            // Main thread for user input and sending encrypted messages
            String message;
            while (true) {
                System.out.print("\r"); // Move the cursor to the start of the line
                System.out.print("You: ");
                message = userEntry.readLine();

                if (message.equals("STOP")) {
                    try {
                        System.out.println("\n* Closing connection... *");

                        link.close();
                    } catch (IOException e) {
                        System.out.println("Unable to disconnect/close!");
                        System.exit(1);
                    }
                }

                // Encrypt the message before sending
                try {
                    String encryptedMessage = Encrypt.encrypt(message, secretKey);
                    out.println(encryptedMessage);
                } catch (Exception e) {
                    System.out.println("Error encrypting message.");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to send login or register request to the server
    public static String performAction(String action, String username, String password) {
        try {
            // Send the action type (for example, "login" or "register") to the server
            out.println(action);

            // Send the username and password to the server
            out.println(username);
            out.println(password);

            // Receive and return the server's response
            return in.readLine();

        } catch (IOException e) {
            e.printStackTrace();
            return "Connection error!";
        }
    }

    // Method to close the connection when the program ends
    public static void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (connection != null) {
                connection.close();
            }
            System.out.println("\nConnection closed.");
        } catch (IOException e) {
            System.err.println("\nError closing the connection!");
        }
    }
}
