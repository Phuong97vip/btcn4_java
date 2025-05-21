package com.chatapp.client;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.chatapp.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (!username.isEmpty() && !password.isEmpty()) {
                handleLogin(username, password);
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both username and password");
            }
        });
        registerButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (!username.isEmpty() && !password.isEmpty()) {
                handleRegister(username, password);
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both username and password");
            }
        });
        passwordField.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (!username.isEmpty() && !password.isEmpty()) {
                handleLogin(username, password);
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both username and password");
            }
        });

        add(mainPanel);
        System.out.println("[LoginWindow] GUI components initialized successfully");
    }

    private void handleLogin(String username, String password) {
        System.out.println("[LoginWindow] Attempting to login user: " + username);
        Message loginMsg = new Message("LOGIN", password);
        loginMsg.setSender(username);
        out.println(gson.toJson(loginMsg));
        System.out.println("[LoginWindow] Login request sent to server");
    }

    private void handleRegister(String username, String password) {
        System.out.println("[LoginWindow] Attempting to register user: " + username);
        Message message = new Message("REGISTER", password);
        message.setSender(username);
        out.println(gson.toJson(message));
        System.out.println("[LoginWindow] Registration request sent to server");
    }

    public void handleServerResponse(JsonObject response) {
        System.out.println("[LoginWindow] Received response from server: " + response.toString());
        String status = response.get("status").getAsString();
        String message = response.get("message").getAsString();

        if (status.equals("success")) {
            System.out.println("[LoginWindow] Operation successful: " + message);
            if (message.equals("Registration successful")) {
                // After successful registration, automatically login
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                handleLogin(username, password);
            } else {
                // Normal login success
                String username = usernameField.getText().trim();
                chatClient.setCurrentUser(username);
                chatClient.showMainChatWindow();
                dispose();
            }
        } else {
            System.out.println("[LoginWindow] Operation failed: " + message);
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
} 