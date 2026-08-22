package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;

public interface AIService {
    ChatResponse generateResponse(ChatRequest request, String context);
}
