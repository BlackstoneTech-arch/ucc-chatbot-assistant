package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.Contact;
import com.ucc.chatbot.model.FAQ;
import com.ucc.chatbot.repository.ContactRepository;
import com.ucc.chatbot.repository.FAQRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    }    @GetMapping("/api/contacts")
    public ResponseEntity<List<Contact>> listContacts() {
        return ResponseEntity.ok(contactRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }
}
