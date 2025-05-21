package com.chatapp.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import com.chatapp.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class MainChatWindow extends JFrame {
    private static final Gson gson = new Gson();
    private final String currentUser;
    private final PrintWriter out;
    private JTabbedPane chatTabs;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JList<String> groupList;
    private DefaultListModel<String> groupListModel;
    private Map<String, ChatPanel> chatPanels;
    private Map<String, GroupChatPanel> groupChatPanels;
    private Map<String, Boolean> unreadMessages;

    public MainChatWindow(String currentUser, PrintWriter out) {
        this.currentUser = currentUser;
        this.out = out;
        this.chatPanels = new HashMap<>();
        this.groupChatPanels = new HashMap<>();
        this.unreadMessages = new HashMap<>();

        setTitle("Chat - " + currentUser);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        initializeGUI();
    }

    private void initializeGUI() {
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left panel for user and group lists
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Users"));

        // Group list
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane groupScrollPane = new JScrollPane(groupList);
        groupScrollPane.setBorder(BorderFactory.createTitledBorder("Groups"));

        // Tabbed pane for user and group lists
        JTabbedPane listTabs = new JTabbedPane();
        listTabs.addTab("Users", userScrollPane);
        listTabs.addTab("Groups", groupScrollPane);
        leftPanel.add(listTabs, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            Message getAllUsersMsg = new Message("GET_ALL_USERS", "");
            getAllUsersMsg.setSender(currentUser);
            out.println(gson.toJson(getAllUsersMsg));
        });
        leftPanel.add(refreshButton, BorderLayout.SOUTH);

        // Chat tabs
        chatTabs = new JTabbedPane();
        chatTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Add panels to main panel
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(chatTabs, BorderLayout.CENTER);

        // Add double-click listeners
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null) {
                        openChat(selectedUser);
                    }
                }
            }
        });

        groupList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null) {
                        openGroupChat(selectedGroup);
                    }
                }
            }
        });

        add(mainPanel);
    }

    private void openChat(String username) {
        if (!chatPanels.containsKey(username)) {
            ChatPanel chatPanel = new ChatPanel(username, currentUser, out);
            chatPanels.put(username, chatPanel);
            
            // Create tab with close button
            JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            tabPanel.setOpaque(false);
            
            // Add username label
            JLabel nameLabel = new JLabel(username);
            tabPanel.add(nameLabel);
            
            // Add close button
            JButton closeButton = new JButton("×");
            closeButton.setFont(new Font("Arial", Font.BOLD, 14));
            closeButton.setPreferredSize(new Dimension(20, 20));
            closeButton.setBorderPainted(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setFocusPainted(false);
            closeButton.addActionListener(e -> closeChat(username));
            tabPanel.add(closeButton);
            
            // Add tab and set its component
            chatTabs.addTab(username, chatPanel);
            int tabIndex = chatTabs.getTabCount() - 1;
            chatTabs.setTabComponentAt(tabIndex, tabPanel);
            unreadMessages.put(username, false);
            
            // Select the new tab
            chatTabs.setSelectedIndex(tabIndex);
        } else {
            // If tab exists, just select it
            for (int i = 0; i < chatTabs.getTabCount(); i++) {
                if (chatTabs.getComponentAt(i) == chatPanels.get(username)) {
                    chatTabs.setSelectedIndex(i);
                    break;
                }
            }
        }
        unreadMessages.put(username, false);
        updateTabTitle(username);
    }

    private void openGroupChat(String groupName) {
        if (!groupChatPanels.containsKey(groupName)) {
            GroupChatPanel groupChatPanel = new GroupChatPanel(groupName, currentUser, out);
            groupChatPanels.put(groupName, groupChatPanel);
            
            // Create tab with close button
            JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            tabPanel.setOpaque(false);
            
            // Add group name label
            JLabel nameLabel = new JLabel(groupName);
            tabPanel.add(nameLabel);
            
            // Add close button
            JButton closeButton = new JButton("×");
            closeButton.setFont(new Font("Arial", Font.BOLD, 14));
            closeButton.setPreferredSize(new Dimension(20, 20));
            closeButton.setBorderPainted(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setFocusPainted(false);
            closeButton.addActionListener(e -> closeGroupChat(groupName));
            tabPanel.add(closeButton);
            
            // Add tab and set its component
            chatTabs.addTab(groupName, groupChatPanel);
            int tabIndex = chatTabs.getTabCount() - 1;
            chatTabs.setTabComponentAt(tabIndex, tabPanel);
            unreadMessages.put(groupName, false);
            
            // Select the new tab
            chatTabs.setSelectedIndex(tabIndex);
        } else {
            // If tab exists, just select it
            for (int i = 0; i < chatTabs.getTabCount(); i++) {
                if (chatTabs.getComponentAt(i) == groupChatPanels.get(groupName)) {
                    chatTabs.setSelectedIndex(i);
                    break;
                }
            }
        }
        unreadMessages.put(groupName, false);
        updateTabTitle(groupName);
    }

    private void closeChat(String username) {
        ChatPanel panel = chatPanels.remove(username);
        if (panel != null) {
            chatTabs.remove(panel);
            unreadMessages.remove(username);
        }
    }

    private void closeGroupChat(String groupName) {
        GroupChatPanel panel = groupChatPanels.remove(groupName);
        if (panel != null) {
            chatTabs.remove(panel);
            unreadMessages.remove(groupName);
        }
    }

    private void updateTabTitle(String title) {
        for (int i = 0; i < chatTabs.getTabCount(); i++) {
            Component tabComponent = chatTabs.getTabComponentAt(i);
            if (tabComponent instanceof JPanel) {
                JPanel tabPanel = (JPanel) tabComponent;
                for (Component comp : tabPanel.getComponents()) {
                    if (comp instanceof JLabel) {
                        JLabel label = (JLabel) comp;
                        if (label.getText().equals(title)) {
                            if (unreadMessages.getOrDefault(title, false)) {
                                label.setText(title + " (new)");
                            } else {
                                label.setText(title);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public void updateUserList(JsonArray users) {
        userListModel.clear();
        for (int i = 0; i < users.size(); i++) {
            String username = users.get(i).getAsString();
            if (!username.equals(currentUser)) {
                userListModel.addElement(username);
            }
        }
    }

    public void updateGroupList(JsonArray groups) {
        groupListModel.clear();
        for (int i = 0; i < groups.size(); i++) {
            groupListModel.addElement(groups.get(i).getAsString());
        }
    }

    public void handleChatMessage(JsonObject message) {
        String sender = message.get("sender").getAsString();
        String recipient = message.get("recipient").getAsString();
        String content = message.get("content").getAsString();
        
        // If message is for current user
        if (recipient.equals(currentUser)) {
            // If chat window is not open, open it
            if (!chatPanels.containsKey(sender)) {
                openChat(sender);
            }
            
            // Add message to chat panel
            ChatPanel chatPanel = chatPanels.get(sender);
            if (chatPanel != null) {
                chatPanel.addMessage(sender, content);
            }
            
            // If chat window is not active, mark as unread
            if (chatTabs.getSelectedComponent() != chatPanel) {
                unreadMessages.put(sender, true);
                updateTabTitle(sender);
            }
        }
        // If message is from current user
        else if (sender.equals(currentUser)) {
            // If chat window is not open, open it
            if (!chatPanels.containsKey(recipient)) {
                openChat(recipient);
            }
            
            // Add message to chat panel
            ChatPanel chatPanel = chatPanels.get(recipient);
            if (chatPanel != null) {
                chatPanel.addMessage(sender, content);
            }
        }
    }
} 