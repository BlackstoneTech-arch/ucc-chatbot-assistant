package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.model.Feedback;

public interface AIService {
    ChatResponse generateResponse(ChatRequest request, String context);
    Feedback recordFeedback(String sessionId, String messageId, int rating, String comment);
}
