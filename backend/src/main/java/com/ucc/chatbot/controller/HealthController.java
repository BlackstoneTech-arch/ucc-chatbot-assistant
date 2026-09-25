package com.ucc.chatbot.controller;

import com.ucc.chatbot.service.AIService;
import com.ucc.chatbot.service.HybridRetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AIService aiService;

    @Autowired
    private HybridRetrievalService hybridRetrievalService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("version", "1.0.0");
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("database", one != null && one == 1 ? "UP" : "DOWN");
        } catch (Exception e) {
            result.put("database", "DOWN");
            result.put("databaseError", e.getMessage());
        }
        result.put("aiService", healthAi());
        result.put("retrieval", healthRetrieval());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health/ai")
    public ResponseEntity<Map<String, Object>> healthAi() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            aiService.generateResponse(new com.ucc.chatbot.dto.ChatRequest() {{
                setMessage("health-check");
                setConversationId("health");
                setLanguage("en");
            }}, "health");
            result.put("status", "UP");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health/retrieval")
    public ResponseEntity<Map<String, Object>> healthRetrieval() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            hybridRetrievalService.findBest("health-check");
            result.put("status", "UP");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
