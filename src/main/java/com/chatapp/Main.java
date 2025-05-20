package com.chatapp;

import com.chatapp.client.ChatClient;
import com.chatapp.server.ChatServer;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("server")) {
            ChatServer.main(args);
        } else {
            ChatClient.main(args);
        }
    }
} 