package com.chatapp.client;

import com.chatapp.model.Message;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.util.*;

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

    public MainChatWindow(String currentUser, PrintWriter out) {
        this.currentUser = currentUser;
        this.out = out;
        this.chatPanels = new HashMap<>();
        this.groupChatPanels = new HashMap<>();

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
            chatTabs.addTab(username, chatPanel);
        }
        chatTabs.setSelectedComponent(chatPanels.get(username));
    }

    private void openGroupChat(String groupName) {
        if (!groupChatPanels.containsKey(groupName)) {
            GroupChatPanel groupChatPanel = new GroupChatPanel(groupName, currentUser, out);
            groupChatPanels.put(groupName, groupChatPanel);
            chatTabs.addTab(groupName, groupChatPanel);
        }
        chatTabs.setSelectedComponent(groupChatPanels.get(groupName));
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
        String content = message.get("content").getAsString();
        String recipient = message.get("recipient").getAsString();

        if (message.has("isGroup") && message.get("isGroup").getAsBoolean()) {
            if (groupChatPanels.containsKey(recipient)) {
                groupChatPanels.get(recipient).addMessage(sender, content);
            }
        } else {
            if (chatPanels.containsKey(sender)) {
                chatPanels.get(sender).addMessage(sender, content);
            }
        }
    }
} 