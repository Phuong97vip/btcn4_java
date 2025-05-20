package com.chatapp.database;

import com.chatapp.model.Message;
import com.chatapp.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            createTables();
        } catch (ClassNotFoundException e) {
            System.out.println("[DatabaseManager] SQLite JDBC driver not found: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    private void createTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create messages table
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "sender_id INTEGER NOT NULL," +
                    "recipient_id INTEGER," +
                    "group_id INTEGER," +
                    "content TEXT NOT NULL," +
                    "is_file BOOLEAN DEFAULT 0," +
                    "file_name TEXT," +
                    "file_content TEXT," +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (sender_id) REFERENCES users(id)," +
                    "FOREIGN KEY (recipient_id) REFERENCES users(id))");

            // Create groups table
            stmt.execute("CREATE TABLE IF NOT EXISTS groups (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT UNIQUE NOT NULL," +
                    "created_by INTEGER NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (created_by) REFERENCES users(id))");

            // Create group_members table
            stmt.execute("CREATE TABLE IF NOT EXISTS group_members (" +
                    "group_id INTEGER NOT NULL," +
                    "user_id INTEGER NOT NULL," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (group_id, user_id)," +
                    "FOREIGN KEY (group_id) REFERENCES groups(id)," +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean registerUser(String username, String password) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password) VALUES (?, ?)")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error registering user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public User authenticateUser(String username, String password) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM users WHERE username = ? AND password = ?")) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User(username, password);
                user.setId(rs.getInt("id"));
                return user;
            }
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error authenticating user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void saveMessage(int senderId, int recipientId, Integer groupId, String content,
                          boolean isFile, String fileName, String fileContent) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO messages (sender_id, recipient_id, group_id, content, is_file, file_name, file_content) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, recipientId);
            pstmt.setObject(3, groupId);
            pstmt.setString(4, content);
            pstmt.setBoolean(5, isFile);
            pstmt.setString(6, fileName);
            pstmt.setString(7, fileContent);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error saving message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int createGroup(String name, int createdBy) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO groups (name, created_by) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, createdBy);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int groupId = rs.getInt(1);
                // Add creator as group member
                addGroupMember(groupId, createdBy);
                return groupId;
            }
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error creating group: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private void addGroupMember(int groupId, int userId) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO group_members (group_id, user_id) VALUES (?, ?)")) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error adding group member: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username FROM users")) {
            
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }
} 