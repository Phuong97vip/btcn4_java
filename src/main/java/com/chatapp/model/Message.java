package com.chatapp.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Message {
    private String type;
    private String content;
    private String sender;
    private String recipient;
    private String groupId;
    private Date timestamp;
    private boolean isFile;
    private String fileName;
    private String fileContent;
    private boolean isGroup;
    private Set<String> deleteUsers; // Track which users have deleted this message

    public Message(String type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = new Date();
        this.deleteUsers = new HashSet<>();
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public Set<String> getDeleteUsers() {
        return deleteUsers;
    }

    public void setDeleteUsers(Set<String> deleteUsers) {
        this.deleteUsers = deleteUsers;
    }

    public void addDeleteUser(String username) {
        this.deleteUsers.add(username);
    }

    public boolean isDeletedByUser(String username) {
        return this.deleteUsers.contains(username);
    }
} 