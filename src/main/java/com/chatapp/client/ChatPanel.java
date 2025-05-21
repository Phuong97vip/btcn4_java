package com.chatapp.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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
            addMessage(currentUser, messageText);
            
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
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        Message message = new Message("CHAT", content);
        message.setSender(currentUser);
        message.setRecipient(recipient);
        message.setFile(true);
        message.setFileName(file.getName());
        message.setFileContent(content);
        out.println(gson.toJson(message));
    }

    protected void clearChat() {
        chatArea.setText("");
    }

    public void addMessage(String sender, String content) {
        System.out.println("[ChatPanel] Adding message from " + sender + ": " + content);
        String prefix = sender.equals(currentUser) ? "You" : sender;
        String message = prefix + ": " + content;
        messages.add(message);
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void loadChatHistory(JsonArray history) {
        System.out.println("[ChatPanel] Loading chat history for " + recipient);
        chatArea.setText("");
        messages.clear();
        
        for (int i = 0; i < history.size(); i++) {
            JsonObject msg = history.get(i).getAsJsonObject();
            String sender = msg.get("sender").getAsString();
            String content = msg.get("content").getAsString();
            addMessage(sender, content);
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