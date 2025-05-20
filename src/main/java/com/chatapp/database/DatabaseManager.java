package com.chatapp.database;

import com.chatapp.model.Message;
import com.chatapp.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
            createTables();
        } catch (Exception e) {
            System.out.println("[DatabaseManager] Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void createTables() {
        try {
            Statement stmt = connection.createStatement();
            
            // Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL" +
                ")");

            // Messages table
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender_id INTEGER NOT NULL," +
                "recipient_id INTEGER," +
                "group_id INTEGER," +
                "content TEXT NOT NULL," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "is_file BOOLEAN DEFAULT 0," +
                "file_name TEXT," +
                "file_content TEXT," +
                "FOREIGN KEY (sender_id) REFERENCES users(id)," +
                "FOREIGN KEY (recipient_id) REFERENCES users(id)" +
                ")");

            // Groups table
            stmt.execute("CREATE TABLE IF NOT EXISTS groups (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE NOT NULL," +
                "creator_id INTEGER NOT NULL," +
                "FOREIGN KEY (creator_id) REFERENCES users(id)" +
                ")");

            // Group members table
            stmt.execute("CREATE TABLE IF NOT EXISTS group_members (" +
                "group_id INTEGER NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "PRIMARY KEY (group_id, user_id)," +
                "FOREIGN KEY (group_id) REFERENCES groups(id)," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")");

            stmt.close();
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean registerUser(String username, String password) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            stmt.close();
            return true;
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error registering user: " + e.getMessage());
            return false;
        }
    }

    public User authenticateUser(String username, String password) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password = ?");
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(username, password);
                user.setId(rs.getInt("id"));
                rs.close();
                stmt.close();
                return user;
            }
            rs.close();
            stmt.close();
            return null;
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error authenticating user: " + e.getMessage());
            return null;
        }
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username FROM users");
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error getting all users: " + e.getMessage());
        }
        return users;
    }

    public void saveMessage(int senderId, int recipientId, Integer groupId, String content,
                          boolean isFile, String fileName, String fileContent) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO messages (sender_id, recipient_id, group_id, content, is_file, file_name, file_content) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, senderId);
            stmt.setInt(2, recipientId);
            if (groupId != null) {
                stmt.setInt(3, groupId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, content);
            stmt.setBoolean(5, isFile);
            stmt.setString(6, fileName);
            stmt.setString(7, fileContent);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error saving message: " + e.getMessage());
        }
    }

    public int createGroup(String name, int creatorId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO groups (name, creator_id) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, name);
            stmt.setInt(2, creatorId);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int groupId = rs.getInt(1);
                rs.close();
                stmt.close();

                // Add creator as group member
                stmt = connection.prepareStatement(
                    "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)");
                stmt.setInt(1, groupId);
                stmt.setInt(2, creatorId);
                stmt.executeUpdate();
                stmt.close();

                return groupId;
            }
            rs.close();
            stmt.close();
            return -1;
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error creating group: " + e.getMessage());
            return -1;
        }
    }

    public List<Message> getChatHistory(int userId1, int userId2) {
        List<Message> messages = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM messages WHERE " +
                "(sender_id = ? AND recipient_id = ?) OR " +
                "(sender_id = ? AND recipient_id = ?) " +
                "ORDER BY timestamp");
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            stmt.setInt(3, userId2);
            stmt.setInt(4, userId1);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Message message = new Message("CHAT", rs.getString("content"));
                message.setSender(getUsername(rs.getInt("sender_id")));
                message.setRecipient(getUsername(rs.getInt("recipient_id")));
                message.setTimestamp(rs.getTimestamp("timestamp"));
                message.setFile(rs.getBoolean("is_file"));
                message.setFileName(rs.getString("file_name"));
                message.setFileContent(rs.getString("file_content"));
                messages.add(message);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error getting chat history: " + e.getMessage());
        }
        return messages;
    }

    private String getUsername(int userId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT username FROM users WHERE id = ?");
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                rs.close();
                stmt.close();
                return username;
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error getting username: " + e.getMessage());
        }
        return null;
    }
} 