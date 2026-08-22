package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.model.Message;
import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.repository.ConversationRepository;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import com.ucc.chatbot.service.AIService;
import com.ucc.chatbot.service.ConversationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements com.ucc.chatbot.service.ChatService {

    private final AIService aiService;
    private final ConversationService conversationService;
    private final KnowledgeDocumentRepository knowledgeRepository;

    public ChatServiceImpl(AIService aiService, ConversationService conversationService, KnowledgeDocumentRepository knowledgeRepository) {
        this.aiService = aiService;
        this.conversationService = conversationService;
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public ChatResponse processMessage(ChatRequest request) {
        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            return ChatResponse.builder()
                    .answer("Please enter a message.")
                    .language(request.getLanguage() != null ? request.getLanguage() : "en")
                    .conversationId(request.getConversationId())
                    .confidence(0.0)
                    .escalationRequired(false)
                    .build();
        }

        Conversation conversation = conversationService.getOrCreateConversation(
                request.getConversationId(),
                null
        ).orElse(null);

        if (conversation != null) {
            conversationService.saveMessage(conversation, "user", message);
        }

        String context = retrieveContext(message);
        ChatResponse response = aiService.generateResponse(request, context);

        if (conversation != null && response.getAnswer() != null) {
            conversationService.saveMessage(conversation, "assistant", response.getAnswer());
        }

        return response;
    }

    private String retrieveContext(String message) {
        String lowerMessage = message.toLowerCase();
        List<KnowledgeDocument> documents = knowledgeRepository.findByIsActiveTrue();

        StringBuilder context = new StringBuilder();
        for (KnowledgeDocument doc : documents) {
            if (doc.getContent() != null && lowerMessage.contains(doc.getCategory() != null ? doc.getCategory().toLowerCase() : "")) {
                context.append(doc.getTitle()).append(": ").append(doc.getContent()).append("\n\n");
            }
        }
        return context.toString();
    }
}
