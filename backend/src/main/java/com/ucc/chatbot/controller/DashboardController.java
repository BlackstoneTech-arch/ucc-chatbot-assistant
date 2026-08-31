package com.ucc.chatbot.controller;

import com.ucc.chatbot.repository.*;
import com.ucc.chatbot.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR','VIEWER')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class DashboardController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final KnowledgeDocumentRepository knowledgeRepository;
    private final AILogRepository aiLogRepository;
    private final FeedbackRepository feedbackRepository;
    private final EscalationRepository escalationRepository;
    private final SystemLogRepository systemLogRepository;

    @Autowired
    public DashboardController(ConversationRepository conversationRepository,
                                MessageRepository messageRepository,
                                UserRepository userRepository,
                                KnowledgeDocumentRepository knowledgeRepository,
                                AILogRepository aiLogRepository,
                                FeedbackRepository feedbackRepository,
                                EscalationRepository escalationRepository,
                                SystemLogRepository systemLogRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.aiLogRepository = aiLogRepository;
        this.feedbackRepository = feedbackRepository;
        this.escalationRepository = escalationRepository;
        this.systemLogRepository = systemLogRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusDays(30).atStartOfDay();

        result.put("totalConversations", conversationRepository.count());
        result.put("activeConversations", conversationRepository.countByIsActiveTrue());
        result.put("todayConversations", conversationRepository.countByStartedAtAfter(startOfDay));
        result.put("weekConversations", conversationRepository.countByStartedAtAfter(startOfWeek));
        result.put("monthConversations", conversationRepository.countByStartedAtAfter(startOfMonth));
        result.put("totalMessages", messageRepository.count());
        result.put("todayMessages", messageRepository.countByCreatedAtAfter(startOfDay));
        result.put("totalUsers", userRepository.count());
        result.put("activeUsers", userRepository.countByRole("ADMIN") + userRepository.countByRole("STAFF") + userRepository.countByRole("EDITOR"));
        result.put("totalKnowledgeDocs", knowledgeRepository.count());
        result.put("approvedKnowledgeDocs", knowledgeRepository.countByApprovalStatusAndIsActiveTrue("APPROVED"));
        result.put("pendingKnowledgeDocs", knowledgeRepository.countByApprovalStatusAndIsActiveTrue("PENDING"));
        result.put("totalAILogs", aiLogRepository.count());
        result.put("todayAILogs", aiLogRepository.countByCreatedAtAfter(startOfDay));
        result.put("totalFeedback", feedbackRepository.count());
        result.put("pendingEscalations", escalationRepository.findByStatusOrderByCreatedAtDesc("OPEN").size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/activity")
    public ResponseEntity<List<Map<String, Object>>> activity() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<AILog> recent = aiLogRepository.findTop100ByOrderByCreatedAtDesc();
        for (int i = 0; i < Math.min(20, recent.size()); i++) {
            AILog log = recent.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", log.getType());
            item.put("action", log.getAction());
            item.put("user", log.getUserEmail() != null ? log.getUserEmail() : "anonymous");
            item.put("message", log.getMessage());
            item.put("status", log.getStatus());
            item.put("createdAt", log.getCreatedAt());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("database", "UP");
        result.put("aiService", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("version", "1.0.0");
        return ResponseEntity.ok(result);
    }
}
