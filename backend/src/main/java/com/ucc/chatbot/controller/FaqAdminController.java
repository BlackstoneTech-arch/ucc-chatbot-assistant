package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/faqs")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class FaqAdminController {

    private final FAQRepository faqRepository;
    private final ContactRepository contactRepository;

    @Autowired
    public FaqAdminController(FAQRepository faqRepository, ContactRepository contactRepository) {
        this.faqRepository = faqRepository;
        this.contactRepository = contactRepository;
    }

    @GetMapping
    public ResponseEntity<List<FAQ>> list() {
        return ResponseEntity.ok(faqRepository.findAllByOrderByPriorityDesc());
    }

    @PostMapping
    public ResponseEntity<FAQ> create(@RequestBody FAQ faq) {
        if (faq.getIsPublished() == null) faq.setIsPublished(true);
        return ResponseEntity.ok(faqRepository.save(faq));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FAQ> update(@PathVariable String id, @RequestBody FAQ faq) {
        FAQ existing = faqRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setQuestion(faq.getQuestion());
        existing.setAnswer(faq.getAnswer());
        existing.setCategory(faq.getCategory());
        existing.setPriority(faq.getPriority());
        existing.setIsPublished(faq.getIsPublished());
        return ResponseEntity.ok(faqRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        faqRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/contacts")
    public ResponseEntity<List<Contact>> contacts() {
        return ResponseEntity.ok(contactRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }
}
