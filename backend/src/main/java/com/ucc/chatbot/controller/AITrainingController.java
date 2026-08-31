package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import com.ucc.chatbot.service.AIService;
import com.ucc.chatbot.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class AITrainingController {

    private final AIPromptRepository promptRepository;
    private final AISettingRepository settingRepository;
    private final AILogRepository logRepository;
    private final AIService aiService;
    private final EncryptionUtil encryptionUtil;

    @Autowired
    public AITrainingController(AIPromptRepository promptRepository,
                                 AISettingRepository settingRepository,
                                 AILogRepository logRepository,
                                 AIService aiService,
                                 EncryptionUtil encryptionUtil) {
        this.promptRepository = promptRepository;
        this.settingRepository = settingRepository;
        this.logRepository = logRepository;
        this.aiService = aiService;
        this.encryptionUtil = encryptionUtil;
    }

    @GetMapping("/prompts")
    public ResponseEntity<List<AIPrompt>> listPrompts() {
        return ResponseEntity.ok(promptRepository.findAll());
    }

    @PostMapping("/prompts")
    public ResponseEntity<AIPrompt> createPrompt(@RequestBody AIPrompt prompt) {
        if (prompt.getIsActive() == null) prompt.setIsActive(true);
        return ResponseEntity.ok(promptRepository.save(prompt));
    }

    @PutMapping("/prompts/{id}")
    public ResponseEntity<AIPrompt> updatePrompt(@PathVariable String id, @RequestBody AIPrompt prompt) {
        AIPrompt existing = promptRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setName(prompt.getName());
        existing.setContent(prompt.getContent());
        existing.setDescription(prompt.getDescription());
        existing.setIsActive(prompt.getIsActive());
        existing.setCategory(prompt.getCategory());
        return ResponseEntity.ok(promptRepository.save(existing));
    }

    @DeleteMapping("/prompts/{id}")
    public ResponseEntity<?> deletePrompt(@PathVariable String id) {
        promptRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/settings")
    public ResponseEntity<List<Map<String, Object>>> listSettings() {
        List<AISetting> all = settingRepository.findByIsActiveTrue();
        List<Map<String, Object>> result = new ArrayList<>();
        for (AISetting s : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", s.getId());
            item.put("key", s.getKey());
            item.put("description", s.getDescription());
            item.put("isEncrypted", s.getIsEncrypted());
            item.put("isActive", s.getIsActive());
            if (s.getIsEncrypted() != null && s.getIsEncrypted()) {
                item.put("value", s.getValue() != null ? "***ENCRYPTED***" : null);
            } else {
                item.put("value", s.getValue());
            }
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<?> updateSetting(@PathVariable String key, @RequestBody Map<String, String> payload) {
        AISetting s = settingRepository.findByKeyAndIsActiveTrue(key).orElse(null);
        if (s == null) {
            s = new AISetting();
            s.setKey(key);
        }
        String value = payload.get("value");
        if (s.getIsEncrypted() != null && s.getIsEncrypted() && value != null && !value.startsWith("***")) {
            s.setValue(encryptionUtil.encrypt(value));
        } else {
            s.setValue(value);
        }
        if (payload.containsKey("description")) s.setDescription(payload.get("description"));
        settingRepository.save(s);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AILog>> listLogs(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(logRepository.findTop100ByOrderByCreatedAtDesc().stream().limit(limit).toList());
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(@RequestBody Map<String, String> payload) {
        String message = payload.getOrDefault("message", "Hello, what programmes do you offer?");
        com.ucc.chatbot.dto.ChatRequest req = new com.ucc.chatbot.dto.ChatRequest();
        req.setMessage(message);
        req.setConversationId("admin-test-" + System.currentTimeMillis());
        com.ucc.chatbot.dto.ChatResponse resp = aiService.generateResponse(req, "");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("response", resp.getAnswer());
        result.put("intent", "");
        result.put("confidence", resp.getConfidence());
        result.put("language", resp.getLanguage());
        return ResponseEntity.ok(result);
    }
}
