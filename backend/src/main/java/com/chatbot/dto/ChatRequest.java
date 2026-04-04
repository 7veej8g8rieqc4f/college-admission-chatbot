package com.chatbot.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {
    private String message;
    private List<ChatMessage> history = new ArrayList<>();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }
}

