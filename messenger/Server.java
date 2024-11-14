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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class Server {

    private static final String CSV_FILE = "users.csv"; // File to store user
    private static ServerSocket servSock;
    private static final int PORT = 1234;
    private static final Map<String, ClientConnection> activeClients = new HashMap<>();
    private static final Map<Integer, String[]> users = new HashMap<>(); // Stores user data in memory
    private static int nextUserId = 1; // Used to assign unique user IDs

    public static void main(String[] args) {
        System.out.println("Server is running...");
        loadUsersFromCSV();

        try {
            servSock = new ServerSocket(PORT);

            while (true) {
                Socket clientSocket = servSock.accept();
                ClientConnection client = new ClientConnection(clientSocket, activeClients);
                Thread clientThread = new Thread(client);
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Error starting server on port " + PORT);
            e.printStackTrace();
        }
    }

    // Load user data from the CSV file into memory
    public static void loadUsersFromCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                // Ensure each line has the correct number of parts
                if (parts.length == 4) {
                    int userId = Integer.parseInt(parts[0]);
                    String username = parts[1];
                    String salt = parts[2];
                    String hashedPassword = parts[3];
                    users.put(userId, new String[]{username, salt, hashedPassword});

                    // Update the next user ID if needed
                    if (userId >= nextUserId) {
                        nextUserId = userId + 1;
                    }
                }
            }
            System.out.println("\nUsers loaded from CSV.");
        } catch (IOException e) {
            System.out.println("\nNo CSV file found. Starting fresh.");
        }
    }

    // Save a new user with a hashed password and salt
    public static synchronized boolean saveUser(String username, String password) {
        // Check if the username already exists
        for (String[] userData : users.values()) {
            if (userData[0].equals(username)) {
                return false;
            }
        }

        int userId = nextUserId++; // Assign a unique ID for the new user
        String salt = generateSalt(); // Create a new salt for this user
        String hashedPassword = hashPassword(password, salt); // Hash the password with the salt

        // Store user details in memory and save to CSV
        users.put(userId, new String[]{username, salt, hashedPassword});
        saveUserToCSV(userId, username, salt, hashedPassword);
        return true;
    }

    // Check if the username and password match (login)
    public static synchronized boolean loginUser(String username, String password) {
        for (String[] userData : users.values()) {
            String storedUsername = userData[0];
            String storedSalt = userData[1];
            String storedHashedPassword = userData[2];

            // Check if the username matches
            if (storedUsername.equals(username)) {
                // Hash the input password with the stored salt and compare with stored hash
                String hashedPassword = hashPassword(password, storedSalt);
                return hashedPassword.equals(storedHashedPassword);
            }
        }
        return false; // Return false if no match is found
    }

    // Append a new user record to the CSV file
    private static void saveUserToCSV(int userId, String username, String salt, String hashedPassword) {
        try (FileWriter fw = new FileWriter(CSV_FILE, true); BufferedWriter bw = new BufferedWriter(fw); PrintWriter out = new PrintWriter(bw)) {

            // Write user details as a new line in the CSV file
            out.println(userId + "," + username + "," + salt + "," + hashedPassword);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Generate a random salt for hashing passwords
    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];

        // Fill the byte array with random bytes
        random.nextBytes(saltBytes);
        // Convert to Base64 for storage
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    // Hash the password with the provided salt
    private static String hashPassword(String password, String salt) {
        try {
            // Use SHA-256 hashing algorithm
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Add the salt to the hash
            md.update(salt.getBytes());
            // Hash the password
            byte[] hashedBytes = md.digest(password.getBytes());
            // Convert to Base64 for storage
            return Base64.getEncoder().encodeToString(hashedBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing error", e);
        }
    }

}
