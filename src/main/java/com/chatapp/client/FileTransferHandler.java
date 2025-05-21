package com.chatapp.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import com.google.gson.JsonObject;

public class FileTransferHandler {
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String DOWNLOADS_DIR = "downloads";
    private final PrintWriter out;
    private final String username;

    public FileTransferHandler(PrintWriter out, String username) {
        this.out = out;
        this.username = username;
        createDownloadsDirectory();
    }

    private void createDownloadsDirectory() {
        try {
            Files.createDirectories(Paths.get(DOWNLOADS_DIR));
        } catch (IOException e) {
            System.err.println("Error creating downloads directory: " + e.getMessage());
        }
    }

    public void sendFile(String receiver) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            if (selectedFile.length() > MAX_FILE_SIZE) {
                JOptionPane.showMessageDialog(null, 
                    "File size exceeds maximum limit of 10MB",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create progress dialog
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            JDialog progressDialog = new JDialog();
            progressDialog.setTitle("Sending File");
            progressDialog.setLayout(new java.awt.BorderLayout());
            progressDialog.add(progressBar, java.awt.BorderLayout.CENTER);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(null);

            // Start file transfer in background
            new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        // Send file metadata
                        JsonObject metadata = new JsonObject();
                        metadata.addProperty("type", "FILE");
                        metadata.addProperty("sender", username);
                        metadata.addProperty("receiver", receiver);
                        metadata.addProperty("filename", selectedFile.getName());
                        metadata.addProperty("filesize", selectedFile.length());
                        out.println(metadata.toString());

                        // Send file content
                        try (FileInputStream fis = new FileInputStream(selectedFile);
                             BufferedInputStream bis = new BufferedInputStream(fis)) {
                            
                            byte[] buffer = new byte[8192];
                            long totalBytesRead = 0;
                            int bytesRead;
                            
                            while ((bytesRead = bis.read(buffer)) != -1) {
                                out.write(new String(buffer, 0, bytesRead));
                                totalBytesRead += bytesRead;
                                int progress = (int) ((totalBytesRead * 100) / selectedFile.length());
                                publish(progress);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null,
                            "Error sending file: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    int progress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(progress);
                    progressBar.setString(progress + "%");
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                }
            }.execute();

            progressDialog.setVisible(true);
        }
    }

    public void receiveFile(String sender, String filename, long filesize, String fileContent) {
        int response = JOptionPane.showConfirmDialog(null,
            "Do you want to receive file '" + filename + "' from " + sender + "?",
            "Incoming File",
            JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.YES_OPTION) {
            File saveFile = new File(DOWNLOADS_DIR, filename);
            
            // Create progress dialog
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            JDialog progressDialog = new JDialog();
            progressDialog.setTitle("Receiving File");
            progressDialog.setLayout(new java.awt.BorderLayout());
            progressDialog.add(progressBar, java.awt.BorderLayout.CENTER);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(null);

            // Start file save in background
            new SwingWorker<Void, Integer>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        byte[] fileData = fileContent.getBytes();
                        try (FileOutputStream fos = new FileOutputStream(saveFile);
                             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                            
                            long totalBytesWritten = 0;
                            int chunkSize = 8192;
                            
                            for (int i = 0; i < fileData.length; i += chunkSize) {
                                int length = Math.min(chunkSize, fileData.length - i);
                                bos.write(fileData, i, length);
                                totalBytesWritten += length;
                                int progress = (int) ((totalBytesWritten * 100) / filesize);
                                publish(progress);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null,
                            "Error saving file: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    int progress = chunks.get(chunks.size() - 1);
                    progressBar.setValue(progress);
                    progressBar.setString(progress + "%");
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(null,
                        "File saved to: " + saveFile.getAbsolutePath(),
                        "File Received",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }.execute();

            progressDialog.setVisible(true);
        }
    }
} 