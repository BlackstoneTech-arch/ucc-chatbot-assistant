package com.ucc.chatbot.dto;

import java.util.List;
import java.util.Map;

public class ChatResponse {
    private String answer;
    private String language;
    private String conversationId;
    private List<Map<String, String>> sources;
    private double confidence;
    private boolean escalationRequired;
    private List<Map<String, Object>> downloads;
    private List<Map<String, String>> quickReplies;

    public ChatResponse() {}

    public ChatResponse(String answer, String language, String conversationId, List<Map<String, String>> sources, double confidence, boolean escalationRequired) {
        this.answer = answer;
        this.language = language;
        this.conversationId = conversationId;
        this.sources = sources;
        this.confidence = confidence;
        this.escalationRequired = escalationRequired;
    }

    public static ChatResponse builder() {
        return new ChatResponse();
    }

    public ChatResponse answer(String answer) { this.answer = answer; return this; }
    public ChatResponse language(String language) { this.language = language; return this; }
    public ChatResponse conversationId(String conversationId) { this.conversationId = conversationId; return this; }
    public ChatResponse sources(List<Map<String, String>> sources) { this.sources = sources; return this; }
    public ChatResponse confidence(double confidence) { this.confidence = confidence; return this; }
    public ChatResponse escalationRequired(boolean escalationRequired) { this.escalationRequired = escalationRequired; return this; }
    public ChatResponse downloads(List<Map<String, Object>> downloads) { this.downloads = downloads; return this; }
    public ChatResponse quickReplies(List<Map<String, String>> quickReplies) { this.quickReplies = quickReplies; return this; }
    public ChatResponse build() { return this; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public List<Map<String, String>> getSources() { return sources; }
    public void setSources(List<Map<String, String>> sources) { this.sources = sources; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public boolean isEscalationRequired() { return escalationRequired; }
    public void setEscalationRequired(boolean escalationRequired) { this.escalationRequired = escalationRequired; }
    public List<Map<String, Object>> getDownloads() { return downloads; }
    public void setDownloads(List<Map<String, Object>> downloads) { this.downloads = downloads; }
    public List<Map<String, String>> getQuickReplies() { return quickReplies; }
    public void setQuickReplies(List<Map<String, String>> quickReplies) { this.quickReplies = quickReplies; }
}
