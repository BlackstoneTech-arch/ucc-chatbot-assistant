package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.Contact;
import com.ucc.chatbot.model.FAQ;
import com.ucc.chatbot.repository.ContactRepository;
import com.ucc.chatbot.repository.FAQRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:5500", "https://uccchatbot.netlify.app", "https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app"})
public class PublicContentController {

    private final FAQRepository faqRepository;
    private final ContactRepository contactRepository;

    @Autowired
    public PublicContentController(FAQRepository faqRepository, ContactRepository contactRepository) {
        this.faqRepository = faqRepository;
        this.contactRepository = contactRepository;
    }

    @GetMapping("/api/faqs")
    public ResponseEntity<List<FAQ>> listFAQs(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(faqRepository.findByCategoryAndIsPublished(category, true));
        }
        return ResponseEntity.ok(faqRepository.findAllByOrderByPriorityDesc().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsPublished())).toList());
    }

    @GetMapping("/api/contacts")
    public ResponseEntity<List<Contact>> listContacts() {
        return ResponseEntity.ok(contactRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }

    /**
     * Verified UCC academic programmes for 2026/2027.
     * Served from the classpath resource data/courses.json so the frontend
     * can retrieve it even when the backend is not deployed.
     */
    @GetMapping("/api/courses")
    public ResponseEntity<Map<String, Object>> listCourses() {
        try {
            ClassPathResource res = new ClassPathResource("data/courses.json");
            if (!res.exists()) {
                return ResponseEntity.ok(Map.of("programmes", List.of(), "intake", "2026/2027"));
            }
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(res.getInputStream());
            Map<String, Object> out = new java.util.LinkedHashMap<>();
            out.put("intake", root.path("intake").asText("2026/2027"));
            out.put("programmes", root.path("programmes"));
            out.put("professionalCourses", root.path("professionalCourses"));
            out.put("contacts", root.path("contacts"));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("programmes", List.of(), "intake", "2026/2027"));
        }
    }
}
