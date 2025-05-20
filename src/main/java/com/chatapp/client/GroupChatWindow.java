package com.chatapp.client;

import com.chatapp.model.Message;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
import java.io.IOException;

public class GroupChatWindow extends JFrame {
    private static final Gson gson = new Gson();
    private final String currentUser;
    private final String groupId;
    private final PrintWriter out;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton historyButton;
    private JButton clearButton;
    private JList<String> membersList;
    private DefaultListModel<String> membersModel;

    public GroupChatWindow(String groupId, String sender, PrintWriter out) {
        this.groupId = groupId;
        this.out = out;
        this.currentUser = sender;

        setTitle("Group Chat: " + groupId);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        initializeGUI();
    }

    private void initializeGUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        // Members list
        membersModel = new DefaultListModel<>();
        membersList = new JList<>(membersModel);
        JScrollPane membersScrollPane = new JScrollPane(membersList);
        membersScrollPane.setBorder(BorderFactory.createTitledBorder("Group Members"));
        membersScrollPane.setPreferredSize(new Dimension(150, 0));

        // Split pane for chat and members
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScrollPane, membersScrollPane);
        splitPane.setDividerLocation(600);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(splitPane, gbc);

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
    }

    private void sendMessage() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            Message message = new Message("GROUP_CHAT", messageText);
            message.setSender(currentUser);
            message.setGroupId(groupId);
            out.println(gson.toJson(message));
            messageField.setText("");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                sendFile(selectedFile);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
            }
        }
    }

    private void sendFile(File file) {
        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            Message message = new Message("GROUP_CHAT", content);
            message.setSender(currentUser);
            message.setGroupId(groupId);
            message.setFile(true);
            message.setFileName(file.getName());
            message.setFileContent(content);
            out.println(gson.toJson(message));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage());
        }
    }

    private void showHistory() {
        Message historyMsg = new Message("GROUP_HISTORY", "");
        historyMsg.setSender(currentUser);
        historyMsg.setGroupId(groupId);
        out.println(gson.toJson(historyMsg));
    }

    private void clearChat() {
        chatArea.setText("");
    }

    public void addMessage(String sender, String content) {
        Message message = new Message("GROUP_CHAT", content);
        message.setSender(sender);
        message.setGroupId(groupId);
        message.setTimestamp(new Date());
        addMessage(message);
    }

    public void addMessage(Message message) {
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
    }

    public void addHistory(List<Message> messages) {
        chatArea.setText("");
        for (Message message : messages) {
            addMessage(message);
        }
    }

    public void updateMembers(List<String> members) {
        membersModel.clear();
        for (String member : members) {
            membersModel.addElement(member);
        }
    }

    public void addMessage(String sender, String content, boolean isFile, String fileName, String fileContent) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String messageText = String.format("[%s] %s: %s", timestamp, sender, content);
        chatArea.append(messageText + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
} 