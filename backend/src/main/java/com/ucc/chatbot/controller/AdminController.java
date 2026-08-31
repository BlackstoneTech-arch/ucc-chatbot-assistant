package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.repository.MessageRepository;
import com.ucc.chatbot.service.ConversationService;
import com.ucc.chatbot.service.KnowledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final ConversationService conversationService;
    private final KnowledgeService knowledgeService;
    private final MessageRepository messageRepository;

    public AdminController(ConversationService conversationService, KnowledgeService knowledgeService, MessageRepository messageRepository) {
        this.conversationService = conversationService;
        this.knowledgeService = knowledgeService;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        List<Conversation> conversations = conversationService.getAllConversations();
        long totalConversations = conversations.size();
        long totalMessages = conversations.stream()
                .mapToLong(c -> messageRepository.countByConversationId(c.getId()))
                .sum();
        long activeDocuments = knowledgeService.getAllActiveDocuments().size();

        return ResponseEntity.ok(Map.of(
                "totalConversations", totalConversations,
                "totalMessages", totalMessages,
                "averageRating", 0.0,
                "totalFeedback", 0,
                "activeDocuments", activeDocuments,
                "errorCount", 0,
                "topIntents", List.of()
        ));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations() {
        return ResponseEntity.ok(conversationService.getAllConversations());
    }
}
