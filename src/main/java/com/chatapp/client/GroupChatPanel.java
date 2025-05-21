package com.chatapp.client;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.chatapp.model.Message;
import com.google.gson.Gson;

public class GroupChatPanel extends ChatPanel {
    private static final Gson gson = new Gson();
    private final String groupName;

    public GroupChatPanel(String groupName, String currentUser, PrintWriter out) {
        super(groupName, currentUser, out);
        this.groupName = groupName;
    }

    @Override
    protected void sendMessage() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            Message message = new Message("GROUP_CHAT", messageText);
            message.setSender(currentUser);
            message.setRecipient(groupName);
            message.setGroup(true);
            out.println(gson.toJson(message));
            messageField.setText("");
        }
    }

    @Override
    protected void sendFile(File file) throws IOException {
        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        Message message = new Message("GROUP_CHAT", content);
        message.setSender(currentUser);
        message.setRecipient(groupName);
        message.setGroup(true);
        message.setFile(true);
        message.setFileName(file.getName());
        message.setFileContent(content);
        out.println(gson.toJson(message));
    }

    @Override
    protected void loadChatHistory() {
        Message historyMsg = new Message("GROUP_HISTORY", "");
        historyMsg.setSender(currentUser);
        historyMsg.setRecipient(groupName);
        historyMsg.setGroup(true);
        out.println(gson.toJson(historyMsg));
    }
} 