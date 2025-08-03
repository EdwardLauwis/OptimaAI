package com.example.optimaai;

import java.util.Date;

public class ChatMessage {
    private final String message;
    private final boolean isUser;
    private Date timestamp;

    public ChatMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = new Date();
    }

    // Getter untuk semua variabel
    public String getMessage() {
        return message;
    }

    public boolean isUser() {
        return isUser;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
