package com.chatapp.server;

import com.chatapp.database.DatabaseManager;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 5000;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groups = new ConcurrentHashMap<>();
    private final DatabaseManager dbManager;
    private final Gson gson;

    public ChatServer() {
        System.out.println("[ChatServer] Initializing server...");
        dbManager = DatabaseManager.getInstance();
        gson = new Gson();
        System.out.println("[ChatServer] Server initialized successfully");
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[ChatServer] Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[ChatServer] New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.out.println("[ChatServer] Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private User currentUser;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                System.out.println("[ClientHandler] Setting up client connection...");
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                System.out.println("[ClientHandler] Client connection setup complete");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("[ClientHandler] Received message: " + inputLine);
                    handleMessage(inputLine);
                }
            } catch (IOException e) {
                System.out.println("[ClientHandler] Error handling client: " + e.getMessage());
                e.printStackTrace();
            } finally {
                cleanup();
            }
        }

        private void handleMessage(String message) {
            System.out.println("[ClientHandler] Handling message of type: " + message);
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            String type = jsonMessage.get("type").getAsString();

            switch (type) {
                case "REGISTER":
                    handleRegister(jsonMessage);
                    break;
                case "LOGIN":
                    handleLogin(jsonMessage);
                    break;
                case "CHAT":
                    handleChat(jsonMessage);
                    break;
                case "GROUP_CREATE":
                    handleGroupCreate(jsonMessage);
                    break;
                case "GROUP_CHAT":
                    handleGroupChat(jsonMessage);
                    break;
                case "GET_ALL_USERS":
                    handleGetAllUsers();
                    break;
                default:
                    System.out.println("[ClientHandler] Unknown message type: " + type);
                    break;
            }
        }

        private void handleRegister(JsonObject message) {
            System.out.println("[ClientHandler] Processing registration for user: " + message.get("sender"));
            JsonObject response = new JsonObject();
            String username = message.get("sender").getAsString();
            String password = message.get("content").getAsString();

            if (dbManager.registerUser(username, password)) {
                System.out.println("[ClientHandler] Registration successful for user: " + username);
                response.addProperty("status", "success");
                response.addProperty("message", "Registration successful");
            } else {
                System.out.println("[ClientHandler] Registration failed for user: " + username);
                response.addProperty("status", "error");
                response.addProperty("message", "Username already exists");
            }
            sendResponse(response);
        }

        private void handleLogin(JsonObject message) {
            System.out.println("[ClientHandler] Processing login for user: " + message.get("sender"));
            JsonObject response = new JsonObject();
            String username = message.get("sender").getAsString();
            String password = message.get("content").getAsString();

            User user = dbManager.authenticateUser(username, password);
            if (user != null) {
                System.out.println("[ClientHandler] Login successful for user: " + username);
                currentUser = user;
                clients.put(username, this);
                response.addProperty("status", "success");
                response.addProperty("message", "Login successful");
                response.addProperty("userId", user.getId());
                sendResponse(response);
                broadcastUserList();
            } else {
                System.out.println("[ClientHandler] Login failed for user: " + username);
                response.addProperty("status", "error");
                response.addProperty("message", "Invalid credentials");
                sendResponse(response);
            }
        }

        private void handleChat(JsonObject message) {
            if (currentUser == null) {
                System.out.println("[ClientHandler] Chat message rejected - no authenticated user");
                return;
            }

            System.out.println("[ClientHandler] Processing chat message from " + currentUser.getUsername() + 
                " to " + message.get("recipient"));

            String recipient = message.get("recipient").getAsString();
            ClientHandler recipientHandler = clients.get(recipient);
            if (recipientHandler != null) {
                message.addProperty("sender", currentUser.getUsername());
                recipientHandler.sendMessage(message);
                System.out.println("[ClientHandler] Message delivered to recipient: " + recipient);
            } else {
                System.out.println("[ClientHandler] Recipient not found: " + recipient);
            }

            // Save message to database
            dbManager.saveMessage(
                currentUser.getId(),
                getUserId(recipient),
                null,
                message.get("content").getAsString(),
                message.has("isFile") && message.get("isFile").getAsBoolean(),
                message.has("fileName") ? message.get("fileName").getAsString() : null,
                message.has("fileContent") ? message.get("fileContent").getAsString() : null
            );
        }

        private void handleGroupCreate(JsonObject message) {
            if (currentUser == null) {
                System.out.println("[ClientHandler] Group creation rejected - no authenticated user");
                return;
            }

            System.out.println("[ClientHandler] Processing group creation request from " + 
                currentUser.getUsername() + " for group: " + message.get("content"));

            String groupName = message.get("content").getAsString();
            int groupId = dbManager.createGroup(groupName, currentUser.getId());
            
            if (groupId != -1) {
                System.out.println("[ClientHandler] Group created successfully: " + groupName);
                groups.put(groupName, new HashSet<>());
                groups.get(groupName).add(currentUser.getUsername());
                
                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("message", "Group created successfully");
                response.addProperty("groupId", groupId);
                sendResponse(response);
                
                broadcastGroupList();
            } else {
                System.out.println("[ClientHandler] Failed to create group: " + groupName);
            }
        }

        private void handleGroupChat(JsonObject message) {
            if (currentUser == null) {
                System.out.println("[ClientHandler] Group chat message rejected - no authenticated user");
                return;
            }

            System.out.println("[ClientHandler] Processing group chat message from " + 
                currentUser.getUsername() + " to group: " + message.get("groupId"));

            String groupName = message.get("groupId").getAsString();
            Set<String> groupMembers = groups.get(groupName);
            if (groupMembers != null) {
                message.addProperty("sender", currentUser.getUsername());
                for (String member : groupMembers) {
                    if (!member.equals(currentUser.getUsername())) {
                        ClientHandler handler = clients.get(member);
                        if (handler != null) {
                            handler.sendMessage(message);
                            System.out.println("[ClientHandler] Message delivered to group member: " + member);
                        }
                    }
                }
            } else {
                System.out.println("[ClientHandler] Group not found: " + groupName);
            }
        }

        private void handleGetAllUsers() {
            System.out.println("[ClientHandler] Getting all users");
            List<String> users = DatabaseManager.getInstance().getAllUsers();
            JsonObject response = new JsonObject();
            response.addProperty("type", "USER_LIST");
            response.addProperty("users", gson.toJson(users));
            out.println(gson.toJson(response));
            System.out.println("[ClientHandler] Sent all users list");
        }

        private void sendMessage(JsonObject message) {
            System.out.println("[ClientHandler] Sending message: " + gson.toJson(message));
            out.println(gson.toJson(message));
        }

        private void sendResponse(JsonObject response) {
            System.out.println("[ClientHandler] Sending response: " + response.toString());
            out.println(response.toString());
        }

        private void cleanup() {
            if (currentUser != null) {
                System.out.println("[ClientHandler] Cleaning up connection for user: " + currentUser.getUsername());
                clients.remove(currentUser.getUsername());
                broadcastUserList();
            }
            try {
                clientSocket.close();
                System.out.println("[ClientHandler] Client socket closed");
            } catch (IOException e) {
                System.out.println("[ClientHandler] Error closing client socket: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private int getUserId(String username) {
            for (ClientHandler handler : clients.values()) {
                if (handler.currentUser != null && handler.currentUser.getUsername().equals(username)) {
                    return handler.currentUser.getId();
                }
            }
            return -1;
        }
    }

    private void broadcastUserList() {
        System.out.println("[ChatServer] Broadcasting user list update");
        JsonObject message = new JsonObject();
        message.addProperty("type", "USER_LIST");
        message.addProperty("users", gson.toJson(new ArrayList<>(clients.keySet())));
        broadcast(message);
    }

    private void broadcastGroupList() {
        System.out.println("[ChatServer] Broadcasting group list update");
        JsonObject message = new JsonObject();
        message.addProperty("type", "GROUP_LIST");
        message.addProperty("groups", gson.toJson(new ArrayList<>(groups.keySet())));
        broadcast(message);
    }

    private void broadcast(JsonObject message) {
        System.out.println("[ChatServer] Broadcasting message to " + clients.size() + " clients");
        for (ClientHandler client : clients.values()) {
            client.sendResponse(message);
        }
    }

    public static void main(String[] args) {
        System.out.println("[ChatServer] Starting chat server...");
        new ChatServer().start();
    }
} 