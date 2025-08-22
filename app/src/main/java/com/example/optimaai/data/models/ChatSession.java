package com.example.optimaai.data.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatSession {

    private String id;
    private String title;
    private Date createdAt;

    public ChatSession() {}

    public ChatSession(String title) {
        this.title = title;
    }

    @Exclude
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }


    @ServerTimestamp
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
