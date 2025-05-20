package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class ChatClient extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final Gson gson = new Gson();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private User currentUser;
    private MainChatWindow mainChatWindow;
    private LoginWindow loginWindow;

    public ChatClient() {
        System.out.println("[DEBUG] Starting ChatClient application");
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        try {
            System.out.println("[DEBUG] Initializing ChatClient...");
            initializeClient();
            System.out.println("[DEBUG] Connecting to server at localhost:5000");
            connectToServer();
            System.out.println("[DEBUG] Successfully connected to server");
            System.out.println("[DEBUG] Initializing GUI...");
            initializeGUI();
            System.out.println("[DEBUG] Starting message listener thread");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize client: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to initialize client: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initializeGUI() {
        loginWindow = new LoginWindow(out, this);
        loginWindow.setVisible(true);
        this.setVisible(false); // Hide main window until login
    }

    private void initializeClient() {
        // No initialization needed
    }

    private void connectToServer() {
        try {
            System.out.println("[DEBUG] Connecting to server at " + SERVER_HOST + ":" + SERVER_PORT);
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("[DEBUG] Successfully connected to server");

            // Start message listener thread
            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to server");
            System.exit(1);
        }
    }

    private void listenForMessages() {
        try {
            System.out.println("[DEBUG] Starting message listener thread");
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[DEBUG] Received message: " + message);
                handleServerResponse(message);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Error in message listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleServerResponse(String response) {
        System.out.println("[ChatClient] Received response from server: " + response);
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        
        // Check if response has type field
        if (jsonResponse.has("type")) {
            String type = jsonResponse.get("type").getAsString();
            switch (type) {
                case "USER_LIST":
                    handleUserList(jsonResponse);
                    break;
                case "GROUP_LIST":
                    handleGroupList(jsonResponse);
                    break;
                case "CHAT":
                    handleChatMessage(jsonResponse);
                    break;
                default:
                    System.out.println("[ChatClient] Unknown message type: " + type);
                    break;
            }
        } else {
            // Forward login/register responses to LoginWindow
            if (loginWindow != null) {
                loginWindow.handleResponse(jsonResponse);
            }
        }
    }

    private void handleChatMessage(JsonObject response) {
        System.out.println("[ChatClient] Handling chat message");
        if (mainChatWindow != null) {
            mainChatWindow.handleChatMessage(response);
        }
    }

    public void setCurrentUser(String username) {
        System.out.println("[ChatClient] Setting current user: " + username);
        this.currentUser = new User(username, "");
    }

    public void showMainChatWindow() {
        if (mainChatWindow == null) {
            mainChatWindow = new MainChatWindow(currentUser.getUsername(), out);
        }
        mainChatWindow.setVisible(true);
    }

    private void handleUserList(JsonObject response) {
        System.out.println("[ChatClient] Handling user list update");
        String usersStr = response.get("users").getAsString();
        JsonArray users = gson.fromJson(usersStr, JsonArray.class);
        if (mainChatWindow != null) {
            mainChatWindow.updateUserList(users);
        }
        System.out.println("[ChatClient] User list updated with " + users.size() + " users");
    }

    private void handleGroupList(JsonObject response) {
        System.out.println("[ChatClient] Handling group list update");
        String groupsStr = response.get("groups").getAsString();
        JsonArray groups = gson.fromJson(groupsStr, JsonArray.class);
        if (mainChatWindow != null) {
            mainChatWindow.updateGroupList(groups);
        }
        System.out.println("[ChatClient] Group list updated with " + groups.size() + " groups");
    }

    public static void main(String[] args) {
        System.out.println("[DEBUG] Starting ChatClient application");
        SwingUtilities.invokeLater(() -> {
            new ChatClient();
        });
    }
} 