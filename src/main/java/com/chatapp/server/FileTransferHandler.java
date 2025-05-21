package com.chatapp.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;

import com.google.gson.JsonObject;

public class FileTransferHandler {
    private static final String UPLOAD_DIR = "src/main/resources/uploads";
    private final Map<String, ClientConnection> clients;

    public FileTransferHandler(Map<String, ClientConnection> clients) {
        this.clients = clients;
    }

    public static String saveFile(String fileName, String base64Content) throws IOException {
        // Create uploads directory if it doesn't exist
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // Generate unique filename
        String uniqueFileName = System.currentTimeMillis() + "_" + fileName;
        Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);
        
        // Decode and save file
        byte[] fileBytes = Base64.getDecoder().decode(base64Content);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(fileBytes);
        }
        
        return uniqueFileName;
    }
    
    public static String getFileContent(String fileName) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        byte[] fileBytes = Files.readAllBytes(filePath);
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    public void handleFileTransfer(JsonObject metadata, String fileContent, ClientConnection sender) {
        String senderUsername = metadata.get("sender").getAsString();
        String receiverUsername = metadata.get("receiver").getAsString();
        String filename = metadata.get("filename").getAsString();
        long filesize = metadata.get("filesize").getAsLong();

        // Save file to server
        String serverFilePath = UPLOAD_DIR + File.separator + filename;
        try {
            Files.write(Paths.get(serverFilePath), fileContent.getBytes());
        } catch (IOException e) {
            System.err.println("Error saving file to server: " + e.getMessage());
            return;
        }

        // Forward file to receiver if online
        ClientConnection receiver = clients.get(receiverUsername);
        if (receiver != null) {
            JsonObject forwardMessage = new JsonObject();
            forwardMessage.addProperty("type", "INCOMING_FILE");
            forwardMessage.addProperty("sender", senderUsername);
            forwardMessage.addProperty("filename", filename);
            forwardMessage.addProperty("filesize", filesize);
            forwardMessage.addProperty("fileContent", fileContent);
            receiver.sendMessage(forwardMessage);
        } else {
            // TODO: Implement offline file storage and notification
            System.out.println("Receiver " + receiverUsername + " is offline. File will be stored for later delivery.");
        }
    }
} 