package com.ucc.chatbot.controller;

import com.ucc.chatbot.dto.FAQRequest;
import com.ucc.chatbot.model.FAQ;
import com.ucc.chatbot.service.RetrievalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faqs")
@CrossOrigin
public class FAQController {

    private final RetrievalService retrievalService;

    public FAQController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @GetMapping
    public ResponseEntity<List<FAQ>> getAllFAQs(Pageable pageable) {
        Page<FAQ> page = retrievalService.getAllFAQs(pageable);
        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FAQ> getFAQById(@PathVariable String id) {
        return retrievalService.getFAQById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FAQ> createFAQ(@Valid @RequestBody FAQRequest request) {
        FAQ faq = new FAQ();
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setCategory(request.getCategory());
        faq.setSourceUrl(request.getSourceUrl());
        faq.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return ResponseEntity.ok(retrievalService.createFAQ(faq));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FAQ> updateFAQ(@PathVariable String id, @Valid @RequestBody FAQRequest request) {
        FAQ faq = new FAQ();
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setCategory(request.getCategory());
        faq.setSourceUrl(request.getSourceUrl());
        faq.setStatus(request.getStatus());
        return ResponseEntity.ok(retrievalService.updateFAQ(id, faq));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFAQ(@PathVariable String id) {
        retrievalService.deleteFAQ(id);
        return ResponseEntity.ok().build();
    }
}
