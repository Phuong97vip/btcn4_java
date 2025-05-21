package com.chatapp.server;

import com.chatapp.model.User;
import com.google.gson.JsonObject;

public interface ClientConnection {
    void sendMessage(JsonObject message);
    void sendResponse(JsonObject response);
    String getUsername();
    User getCurrentUser();
} 