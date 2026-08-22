package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;

public interface ChatService {
    ChatResponse processMessage(ChatRequest request);
}
