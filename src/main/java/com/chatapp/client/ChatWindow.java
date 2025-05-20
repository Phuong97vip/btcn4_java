package com.chatapp.client;

import com.chatapp.model.Message;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatWindow extends JFrame {
    private static final Gson gson = new Gson();
    private final String currentUser;
    private final String recipient;
    private final PrintWriter out;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton historyButton;
    private JButton clearButton;

    public ChatWindow(String recipient, String sender, PrintWriter out) {
        System.out.println("[ChatWindow] Creating chat window for " + sender + " with " + recipient);
        this.recipient = recipient;
        this.currentUser = sender;
        this.out = out;

        setTitle("Chat with " + recipient);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        initializeGUI();
        System.out.println("[ChatWindow] Chat window created successfully");
    }

    private void initializeGUI() {
        System.out.println("[ChatWindow] Initializing GUI components...");
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(chatScrollPane, gbc);

        // Input panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);

        // Message field
        messageField = new JTextField();
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputGbc.weightx = 1.0;
        inputGbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(messageField, inputGbc);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.insets = new Insets(5, 5, 5, 5);
        buttonGbc.fill = GridBagConstraints.HORIZONTAL;

        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        historyButton = new JButton("History");
        clearButton = new JButton("Clear");

        buttonGbc.gridx = 0;
        buttonGbc.gridy = 0;
        buttonGbc.weightx = 1.0;
        buttonsPanel.add(sendButton, buttonGbc);

        buttonGbc.gridx = 1;
        buttonsPanel.add(fileButton, buttonGbc);

        buttonGbc.gridx = 2;
        buttonsPanel.add(historyButton, buttonGbc);

        buttonGbc.gridx = 3;
        buttonsPanel.add(clearButton, buttonGbc);

        inputGbc.gridx = 0;
        inputGbc.gridy = 1;
        inputGbc.gridwidth = 2;
        inputGbc.weightx = 1.0;
        inputPanel.add(buttonsPanel, inputGbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(inputPanel, gbc);

        // Add action listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        historyButton.addActionListener(e -> showHistory());
        clearButton.addActionListener(e -> clearChat());

        add(mainPanel);
        System.out.println("[ChatWindow] GUI components initialized successfully");
    }

    private void sendMessage() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            Message message = new Message("CHAT", messageText);
            message.setSender(currentUser);
            message.setRecipient(recipient);
            out.println(gson.toJson(message));
            messageField.setText("");
        }
    }

    private void sendFile() {
        System.out.println("[ChatWindow] Opening file chooser...");
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("[ChatWindow] Selected file: " + selectedFile.getName());
            try {
                sendFile(selectedFile);
            } catch (IOException e) {
                System.out.println("[ChatWindow] Error reading file: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("[ChatWindow] File selection cancelled");
        }
    }

    private void sendFile(File file) throws IOException {
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        Message message = new Message("CHAT", content);
        message.setSender(currentUser);
        message.setRecipient(recipient);
        message.setFile(true);
        message.setFileName(file.getName());
        message.setFileContent(content);
        out.println(gson.toJson(message));
    }

    private void showHistory() {
        System.out.println("[ChatWindow] Requesting chat history...");
        Message historyMsg = new Message("HISTORY", "");
        historyMsg.setSender(currentUser);
        historyMsg.setRecipient(recipient);
        out.println(gson.toJson(historyMsg));
        System.out.println("[ChatWindow] History request sent");
    }

    private void clearChat() {
        System.out.println("[ChatWindow] Clearing chat area");
        chatArea.setText("");
    }

    public void addMessage(String sender, String content) {
        System.out.println("[ChatWindow] Adding message from " + sender + ": " + content);
        Message message = new Message("CHAT", content);
        message.setSender(sender);
        message.setTimestamp(new Date());
        addMessage(message);
    }

    public void addMessage(Message message) {
        System.out.println("[ChatWindow] Adding message to chat area: " + gson.toJson(message));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timestamp = sdf.format(message.getTimestamp());
        String prefix = message.getSender().equals(currentUser) ? "You" : message.getSender();

        if (message.isFile()) {
            chatArea.append(String.format("[%s] %s sent a file: %s\n", 
                timestamp, prefix, message.getFileName()));
        } else {
            chatArea.append(String.format("[%s] %s: %s\n", 
                timestamp, prefix, message.getContent()));
        }
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
        System.out.println("[ChatWindow] Message added to chat area");
    }

    public void addHistory(List<Message> messages) {
        System.out.println("[ChatWindow] Adding " + messages.size() + " messages to chat history");
        chatArea.setText("");
        for (Message message : messages) {
            addMessage(message);
        }
        System.out.println("[ChatWindow] Chat history loaded");
    }
} 