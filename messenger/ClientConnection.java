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
import java.util.*;

public class ClientConnection implements Runnable {

    private Socket clientSocket;
    private String username;
    private PrintWriter out;
    private BufferedReader in;
    private Map<String, ClientConnection> activeClients;

    public ClientConnection(Socket clientSocket, Map<String, ClientConnection> activeClients) {
        this.clientSocket = clientSocket;
        this.activeClients = activeClients;
    }

    @Override
    public void run() {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Action to be read from the client
            String action;

            // Keep the connection open and process each client command
            while ((action = in.readLine()) != null) {

                // If client wants to register a new account
                if ("register".equalsIgnoreCase(action)) {
                    // Read the inputs
                    username = in.readLine();
                    String password = in.readLine();

                    // Attempt to save the user in the server
                    if (Server.saveUser(username, password)) {
                        out.println("Registration successful.");
                        System.out.println("\nUser saved: " + username);

                    } else {
                        out.println("Registration failed: Username already exists.");
                    }

                    // If client wants to log in
                } else if ("login".equalsIgnoreCase(action)) {
                    // Read the inputs
                    username = in.readLine();
                    String password = in.readLine();

                    // Attempt to log the user in by checking credentials
                    if (Server.loginUser(username, password)) {
                        out.println("Login successful."); // Success message
                        System.out.println("\n" + username + ": is online"); // Server-side log of online user

                        break;
                    } else {
                        out.println("Login failed: Incorrect username or password."); // Failure message
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        startChat(username);
    }

    public void startChat(String username) {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println("Welcome " + username + "!");

            out.println("Enter the username of the user you want to chat with:");
            String targetUsername = in.readLine();

            ClientConnection targetClient;
            synchronized (activeClients) {
                targetClient = activeClients.get(targetUsername);
            }

            if (targetClient == null) {
                out.println("User " + targetUsername + " is not online. Connection closing...");
                return;
            }

            out.println("You are now chatting with " + targetUsername + ". Type 'STOP' to end the chat.");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("STOP")) {
                    out.println("Chat ended. Goodbye!");
                    break;
                }

                System.out.println("\nSender: " + username + "\nReceiver: " + targetUsername + "\nMessage: " + message);

                // Send the encrypted message to the target client
                targetClient.sendMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                synchronized (activeClients) {
                    activeClients.remove(username);
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
