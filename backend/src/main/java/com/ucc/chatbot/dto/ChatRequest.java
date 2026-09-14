package com.ucc.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    @NotBlank(message = "Message is required")
    private String message;

    private String conversationId;

    private String language;

    private java.util.Map<String, Object> user;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public java.util.Map<String, Object> getUser() { return user; }
    public void setUser(java.util.Map<String, Object> user) { this.user = user; }
}
