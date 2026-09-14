package com.ucc.chatbot.controller;

import com.ucc.chatbot.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5500", "https://uccchatbot.netlify.app", "https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app", "https://ucc-chatbot-assistant.blackstone-tech02.workers.dev"})
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','STAFF','EDITOR','VIEWER')")
public class AnalyticsController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final KnowledgeDocumentRepository knowledgeRepository;
    private final FeedbackRepository feedbackRepository;
    private final AILogRepository aiLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final DemoArisRepository arisRepository;
    private final DemoLmsRepository lmsRepository;

    public AnalyticsController(ConversationRepository conversationRepository, MessageRepository messageRepository,
                               UserRepository userRepository, KnowledgeDocumentRepository knowledgeRepository,
                               FeedbackRepository feedbackRepository, AILogRepository aiLogRepository,
                               AuditLogRepository auditLogRepository, DemoArisRepository arisRepository,
                               DemoLmsRepository lmsRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.feedbackRepository = feedbackRepository;
        this.aiLogRepository = aiLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.arisRepository = arisRepository;
        this.lmsRepository = lmsRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalConversations", conversationRepository.count());
        stats.put("totalMessages", messageRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("totalKnowledgeDocs", knowledgeRepository.count());
        stats.put("totalFeedback", feedbackRepository.count());
        stats.put("totalAILogs", aiLogRepository.count());
        stats.put("totalAuditLogs", auditLogRepository.count());
        stats.put("totalDemoAris", arisRepository.count());
        stats.put("totalDemoLms", lmsRepository.count());
        stats.put("timestamp", new Date().toString());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/activity")
    public ResponseEntity<List<Map<String, Object>>> activity() {
        List<Map<String, Object>> activity = new ArrayList<>();
        auditLogRepository.findAll().stream().limit(20).forEach(log -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", "audit");
            entry.put("action", log.getAction());
            entry.put("resource", log.getResourceType());
            entry.put("timestamp", log.getCreatedAt());
            activity.add(entry);
        });
        aiLogRepository.findAll().stream().limit(10).forEach(log -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", "ai");
            entry.put("action", "AI request");
            entry.put("confidence", log.getConfidence());
            entry.put("timestamp", log.getCreatedAt());
            activity.add(entry);
        });
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("database", "UP");
        health.put("aiService", "UP");
        health.put("timestamp", new Date().toString());
        return ResponseEntity.ok(health);
    }
}