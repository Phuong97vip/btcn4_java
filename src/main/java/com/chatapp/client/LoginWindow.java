package com.chatapp.client;

import com.chatapp.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;

public class LoginWindow extends JFrame {
    private static final Gson gson = new Gson();
    private final PrintWriter out;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private ChatClient chatClient;

    public LoginWindow(PrintWriter out, ChatClient chatClient) {
        System.out.println("[LoginWindow] Creating login window...");
        this.out = out;
        this.chatClient = chatClient;

        setTitle("Chat Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);

        initializeGUI();
        System.out.println("[LoginWindow] Login window created successfully");
    }

    private void initializeGUI() {
        System.out.println("[LoginWindow] Initializing GUI components...");
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Username field
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(20);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(usernameField, gbc);

        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(passwordField, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonPanel, gbc);

        // Add action listeners
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());
        passwordField.addActionListener(e -> handleLogin());

        add(mainPanel);
        System.out.println("[LoginWindow] GUI components initialized successfully");
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("[LoginWindow] Login attempt failed - empty username or password");
            JOptionPane.showMessageDialog(this, "Please enter both username and password");
            return;
        }

        System.out.println("[LoginWindow] Attempting to login user: " + username);
        Message message = new Message("LOGIN", password);
        message.setSender(username);
        out.println(gson.toJson(message));
        System.out.println("[LoginWindow] Login request sent to server");
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("[LoginWindow] Registration attempt failed - empty username or password");
            JOptionPane.showMessageDialog(this, "Please enter both username and password");
            return;
        }

        System.out.println("[LoginWindow] Attempting to register user: " + username);
        Message message = new Message("REGISTER", password);
        message.setSender(username);
        out.println(gson.toJson(message));
        System.out.println("[LoginWindow] Registration request sent to server");
    }

    public void handleResponse(JsonObject response) {
        System.out.println("[LoginWindow] Received response from server: " + response.toString());
        String status = response.get("status").getAsString();
        String message = response.get("message").getAsString();

        if (status.equals("success")) {
            System.out.println("[LoginWindow] Operation successful: " + message);
            String username = usernameField.getText().trim();
            
            // Set current user
            chatClient.setCurrentUser(username);
            
            // Request user list
            Message getAllUsersMsg = new Message("GET_ALL_USERS", "");
            getAllUsersMsg.setSender(username);
            out.println(gson.toJson(getAllUsersMsg));
            
            // Show main chat window
            chatClient.showMainChatWindow();
            dispose();
        } else {
            System.out.println("[LoginWindow] Operation failed: " + message);
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
} 