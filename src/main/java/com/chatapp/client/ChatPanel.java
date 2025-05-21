package com.chatapp.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.chatapp.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ChatPanel extends JPanel {
    protected static final Gson gson = new Gson();
    protected final String currentUser;
    protected final String recipient;
    protected final PrintWriter out;
    protected JTextArea chatArea;
    protected JTextField messageField;
    protected JButton sendButton;
    protected JButton fileButton;
    protected JButton clearButton;
    protected ArrayList<String> messages;

    public ChatPanel(String recipient, String currentUser, PrintWriter out) {
        this.recipient = recipient;
        this.currentUser = currentUser;
        this.out = out;
        this.messages = new ArrayList<>();
        
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initializeGUI();
        
        // Request chat history when opening chat
        requestChatHistory();
    }

    protected void initializeGUI() {
        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        clearButton = new JButton("Clear");

        buttonsPanel.add(sendButton);
        buttonsPanel.add(fileButton);
        buttonsPanel.add(clearButton);

        inputPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add components to panel
        add(chatScrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Add action listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        clearButton.addActionListener(e -> clearChat());
    }

    protected void loadChatHistory() {
        Message historyMsg = new Message("HISTORY", "");
        historyMsg.setSender(currentUser);
        historyMsg.setRecipient(recipient);
        out.println(gson.toJson(historyMsg));
    }

    protected void sendMessage() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            Message message = new Message("CHAT", messageText);
            message.setSender(currentUser);
            message.setRecipient(recipient);
            message.setTimestamp(new Date());
            
            // Send message to server
            out.println(gson.toJson(message));
            
            // Display message in sender's chat box
            addMessage(currentUser, messageText, false, "");
            
            // Clear message field
            messageField.setText("");
        }
    }

    protected void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                sendFile(selectedFile);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
            }
        }
    }

    protected void sendFile(File file) throws IOException {
        // Read file and encode to base64
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String encodedContent = Base64.getEncoder().encodeToString(fileBytes);
        
        Message message = new Message("CHAT", encodedContent);
        message.setSender(currentUser);
        message.setRecipient(recipient);
        message.setFile(true);
        message.setFileName(file.getName());
        out.println(gson.toJson(message));
        
        // Add message to chat area
        addMessage(currentUser, encodedContent, true, file.getName());
    }

    protected void clearChat() {
        // Send delete request to server
        Message deleteMsg = new Message("DELETE_MESSAGES", "");
        deleteMsg.setSender(currentUser);
        deleteMsg.setRecipient(recipient);
        out.println(gson.toJson(deleteMsg));
        
        // Clear local chat area
        chatArea.setText("");
        messages.clear();
    }

    public void addMessage(String sender, String content, boolean isFile, String fileName) {
        if (isFile) {
            System.out.println("[ChatPanel] Adding file from " + sender + ": " + fileName);
        } else {
            System.out.println("[ChatPanel] Adding message from " + sender + ": " + content);
        }
        String prefix = sender.equals(currentUser) ? "You" : sender;
        String message = prefix + ": ";
        
        if (isFile) {
            message += "[File: " + fileName + "] ";
            if (!sender.equals(currentUser)) {
                JButton downloadButton = new JButton("Download");
                downloadButton.addActionListener(e -> {
                    // Request file from server
                    JsonObject request = new JsonObject();
                    request.addProperty("type", "GET_FILE");
                    request.addProperty("fileName", fileName);
                    out.println(gson.toJson(request));
                });
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                buttonPanel.add(downloadButton);
                chatArea.append(message + "\n");
                // Add download button to chat area
                chatArea.append("[Download button will appear here]\n");
            } else {
                message += "(Sent)";
            }
        } else {
            message += content;
        }
        
        messages.add(message);
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void downloadFile(String fileContent, String fileName) {
        try {
            // Get user's home directory
            String userHome = System.getProperty("user.home");
            Path downloadPath = Paths.get(userHome, "Downloads", fileName);
            
            // Decode and save file
            byte[] fileBytes = Base64.getDecoder().decode(fileContent);
            try (FileOutputStream fos = new FileOutputStream(downloadPath.toFile())) {
                fos.write(fileBytes);
            }
            
            JOptionPane.showMessageDialog(this, 
                "File downloaded successfully to: " + downloadPath.toString(),
                "Download Complete",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error downloading file: " + e.getMessage(),
                "Download Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadChatHistory(JsonArray history) {
        System.out.println("[ChatPanel] Loading chat history for " + recipient);
        chatArea.setText("");
        messages.clear();
        
        for (int i = 0; i < history.size(); i++) {
            JsonObject msg = history.get(i).getAsJsonObject();
            String sender = msg.get("sender").getAsString();
            String content = msg.get("content").getAsString();
            boolean isFile = msg.has("isFile") && msg.get("isFile").getAsBoolean();
            String fileName = isFile ? msg.get("fileName").getAsString() : "";
            addMessage(sender, content, isFile, fileName);
        }
    }

    private void requestChatHistory() {
        JsonObject request = new JsonObject();
        request.addProperty("type", "GET_CHAT_HISTORY");
        request.addProperty("otherUser", recipient);
        request.addProperty("sender", currentUser);
        out.println(gson.toJson(request));
    }
} 