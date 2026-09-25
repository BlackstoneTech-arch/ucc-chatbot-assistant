package com.ucc.chatbot.controller;

import com.ucc.chatbot.service.ChatService;
import com.ucc.chatbot.service.ConversationService;
import com.ucc.chatbot.repository.MessageRepository;
import com.ucc.chatbot.service.AIService;
import com.ucc.chatbot.service.DocumentCatalogService;
import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.service.ChatServiceImpl;
import com.ucc.chatbot.repository.ConversationRepository;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import com.ucc.chatbot.service.QueryUnderstandingService;
import com.ucc.chatbot.service.HybridRetrievalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chatRequestReturnsExpectedContract() {
        ChatService chatService = mock(ChatService.class);
        ChatController controller = new ChatController(
                chatService,
                mock(ConversationService.class),
                mock(MessageRepository.class),
                mock(AIService.class),
                mock(DocumentCatalogService.class)
        );

        ChatRequest request = new ChatRequest();
        request.setMessage("What programmes does UCC offer?");
        request.setConversationId("abc");
        request.setLanguage("en");

        ChatResponse response = ChatResponse.builder()
                .conversationId("abc")
                .answer("UCC offers BSc IT, BSc CS, and diplomas.")
                .language("en")
                .confidence(0.92)
                .escalationRequired(false)
                .build();

        when(chatService.processMessage(request)).thenReturn(response);

        ResponseEntity<ChatResponse> result = controller.chat(request);
        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals("abc", result.getBody().getConversationId());
        assertTrue(result.getBody().getConfidence() >= 0.9);
    }

    @Test
    void historyReturns401ForAnonymousUser() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        ConversationService conversationService = mock(ConversationService.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        KnowledgeDocumentRepository knowledgeDocumentRepository = mock(KnowledgeDocumentRepository.class);
        QueryUnderstandingService queryUnderstandingService = mock(QueryUnderstandingService.class);
        HybridRetrievalService hybridRetrievalService = mock(HybridRetrievalService.class);
        AIService aiService = mock(AIService.class);
        DocumentCatalogService documentCatalogService = mock(DocumentCatalogService.class);

        ChatServiceImpl chatService = new ChatServiceImpl(aiService, conversationService, queryUnderstandingService, hybridRetrievalService);
        ChatController controller = new ChatController(chatService, conversationService, messageRepository, aiService, documentCatalogService);

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("anonymousUser");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        ResponseEntity<List<Map<String, Object>>> result = controller.getHistory("abc", authentication);
        assertEquals(401, result.getStatusCode().value());
    }
}
