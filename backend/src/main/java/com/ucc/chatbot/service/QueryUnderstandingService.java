package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.QueryUnderstandingResult;

public interface QueryUnderstandingService {
    QueryUnderstandingResult understand(String userMessage, String conversationContext);
}
