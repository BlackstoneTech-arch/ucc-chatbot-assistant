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
    public ChatResponse build() { return this; }

    public String getAnswer() { return answer; }
    public String getLanguage() { return language; }
    public String getConversationId() { return conversationId; }
    public List<Map<String, String>> getSources() { return sources; }
    public double getConfidence() { return confidence; }
    public boolean isEscalationRequired() { return escalationRequired; }
}
