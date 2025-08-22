package com.example.optimaai.data.models;

import java.util.Date;

public class ChatMessage {
    private String message;
    private boolean isUser;
    private Date timestamp;

    public ChatMessage() {}

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = new Date();
    }

    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}