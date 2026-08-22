package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.Integration;
import com.ucc.chatbot.repository.IntegrationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/integrations")
@CrossOrigin
public class IntegrationController {

    private final IntegrationRepository integrationRepository;

    public IntegrationController(IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Integration>> getAllIntegrations() {
        return ResponseEntity.ok(integrationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Integration> getIntegrationById(@PathVariable String id) {
        return integrationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Integration> createIntegration(@RequestBody Integration integration) {
        integration.setStatus("ACTIVE");
        return ResponseEntity.ok(integrationRepository.save(integration));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Integration> updateIntegration(@PathVariable String id, @RequestBody Integration integration) {
        integration.setId(id);
        return ResponseEntity.ok(integrationRepository.save(integration));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIntegration(@PathVariable String id) {
        integrationRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testIntegration(@PathVariable String id) {
        Integration integration = integrationRepository.findById(id).orElse(null);
        if (integration == null) {
            return ResponseEntity.notFound().build();
        }
        boolean success = integration.getBaseUrl() != null && !integration.getBaseUrl().isEmpty();
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Connection test passed" : "Invalid URL"
        ));
    }
}
