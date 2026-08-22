package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.PromptTemplate;
import com.ucc.chatbot.repository.PromptTemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai/prompts")
@CrossOrigin
public class AITrainingController {

    private final PromptTemplateRepository promptTemplateRepository;

    public AITrainingController(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    @GetMapping
    public ResponseEntity<List<PromptTemplate>> getAllPrompts() {
        return ResponseEntity.ok(promptTemplateRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplate> getPromptById(@PathVariable String id) {
        return promptTemplateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PromptTemplate> createPrompt(@RequestBody PromptTemplate prompt) {
        prompt.setIsActive(true);
        return ResponseEntity.ok(promptTemplateRepository.save(prompt));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromptTemplate> updatePrompt(@PathVariable String id, @RequestBody PromptTemplate prompt) {
        prompt.setId(id);
        return ResponseEntity.ok(promptTemplateRepository.save(prompt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(@PathVariable String id) {
        promptTemplateRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
