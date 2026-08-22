package com.ucc.chatbot.controller;

import com.ucc.chatbot.dto.KnowledgeRequest;
import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/knowledge")
@CrossOrigin
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ResponseEntity<List<KnowledgeDocument>> getAllKnowledge(Pageable pageable) {
        Page<KnowledgeDocument> page = knowledgeService.searchDocuments(null, pageable);
        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeDocument> getKnowledgeById(@PathVariable String id) {
        return knowledgeService.getDocumentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<KnowledgeDocument> createKnowledge(@Valid @RequestBody KnowledgeRequest request) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(request.getTitle());
        doc.setCategory(request.getCategory());
        doc.setContent(request.getContent());
        doc.setSourceUrl(request.getSourceUrl());
        doc.setSourceType(request.getSourceType());
        doc.setAcademicYear(request.getAcademicYear());
        doc.setVersion(request.getVersion());
        doc.setApprovalStatus(request.getApprovalStatus());
        doc.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return ResponseEntity.ok(knowledgeService.createDocument(doc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeDocument> updateKnowledge(@PathVariable String id, @Valid @RequestBody KnowledgeRequest request) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(request.getTitle());
        doc.setCategory(request.getCategory());
        doc.setContent(request.getContent());
        doc.setSourceUrl(request.getSourceUrl());
        doc.setSourceType(request.getSourceType());
        doc.setAcademicYear(request.getAcademicYear());
        doc.setVersion(request.getVersion());
        doc.setApprovalStatus(request.getApprovalStatus());
        doc.setIsActive(request.getIsActive());
        return ResponseEntity.ok(knowledgeService.updateDocument(id, doc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable String id) {
        knowledgeService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }
}
