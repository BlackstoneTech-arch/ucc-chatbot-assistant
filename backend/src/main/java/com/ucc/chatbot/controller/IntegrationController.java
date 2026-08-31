package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.PromptTemplate;
import com.ucc.chatbot.model.Integration;
import com.ucc.chatbot.repository.PromptTemplateRepository;
import com.ucc.chatbot.repository.IntegrationRepository;
import com.ucc.chatbot.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/integrations")
@PreAuthorize("hasAnyRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class IntegrationController {

    private final IntegrationRepository integrationRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final EncryptionUtil encryptionUtil;

    @Autowired
    public IntegrationController(IntegrationRepository integrationRepository,
                                   PromptTemplateRepository promptTemplateRepository,
                                   EncryptionUtil encryptionUtil) {
        this.integrationRepository = integrationRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.encryptionUtil = encryptionUtil;
    }

    @GetMapping
    public ResponseEntity<List<Integration>> list() {
        return ResponseEntity.ok(integrationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Integration> get(@PathVariable String id) {
        return ResponseEntity.ok(integrationRepository.findById(id).orElse(null));
    }

    @PostMapping
    public ResponseEntity<Integration> create(@RequestBody Integration integration) {
        if (integration.getApiKey() != null && !integration.getApiKey().isBlank()) {
            integration.setApiKey(encryptionUtil.encrypt(integration.getApiKey()));
        }
        if (integration.getApiSecret() != null && !integration.getApiSecret().isBlank()) {
            integration.setApiSecret(encryptionUtil.encrypt(integration.getApiSecret()));
        }
        integration.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(integrationRepository.save(integration));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integration> update(@PathVariable String id, @RequestBody Integration integration) {
        Integration existing = integrationRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setName(integration.getName());
        existing.setType(integration.getType());
        existing.setBaseUrl(integration.getBaseUrl());
        existing.setWebhookUrl(integration.getWebhookUrl());
        existing.setConfig(integration.getConfig());
        existing.setIsActive(integration.getIsActive());
        existing.setUpdatedAt(LocalDateTime.now());
        if (integration.getApiKey() != null && !integration.getApiKey().isBlank() && !integration.getApiKey().startsWith("***")) {
            existing.setApiKey(encryptionUtil.encrypt(integration.getApiKey()));
        }
        if (integration.getApiSecret() != null && !integration.getApiSecret().isBlank() && !integration.getApiSecret().startsWith("***")) {
            existing.setApiSecret(encryptionUtil.encrypt(integration.getApiSecret()));
        }
        return ResponseEntity.ok(integrationRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        integrationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> test(@PathVariable String id) {
        Integration i = integrationRepository.findById(id).orElse(null);
        if (i == null) return ResponseEntity.notFound().build();
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String url = i.getWebhookUrl() != null && !i.getWebhookUrl().isBlank() ? i.getWebhookUrl() : i.getBaseUrl();
            if (url == null || url.isBlank()) {
                result.put("success", false);
                result.put("message", "No URL configured");
                return ResponseEntity.ok(result);
            }
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            i.setLastTestedAt(LocalDateTime.now());
            i.setLastTestStatus(code >= 200 && code < 400 ? "SUCCESS" : "FAILED");
            i.setLastTestMessage("HTTP " + code);
            integrationRepository.save(i);
            result.put("success", code >= 200 && code < 400);
            result.put("statusCode", code);
            result.put("message", "Integration responded with HTTP " + code);
        } catch (Exception e) {
            i.setLastTestedAt(LocalDateTime.now());
            i.setLastTestStatus("FAILED");
            i.setLastTestMessage(e.getMessage());
            integrationRepository.save(i);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/prompts")
    public ResponseEntity<List<PromptTemplate>> listPrompts() {
        return ResponseEntity.ok(promptTemplateRepository.findAll());
    }

    @PostMapping("/prompts")
    public ResponseEntity<PromptTemplate> createPrompt(@RequestBody PromptTemplate prompt) {
        prompt.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(promptTemplateRepository.save(prompt));
    }

    @PutMapping("/prompts/{id}")
    public ResponseEntity<PromptTemplate> updatePrompt(@PathVariable String id, @RequestBody PromptTemplate prompt) {
        PromptTemplate existing = promptTemplateRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setName(prompt.getName());
        existing.setContent(prompt.getContent());
        existing.setCategory(prompt.getCategory());
        existing.setIsActive(prompt.getIsActive());
        existing.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(promptTemplateRepository.save(existing));
    }

    @DeleteMapping("/prompts/{id}")
    public ResponseEntity<?> deletePrompt(@PathVariable String id) {
        promptTemplateRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
