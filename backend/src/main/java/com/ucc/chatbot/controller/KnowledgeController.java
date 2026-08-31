package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import com.ucc.chatbot.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/knowledge")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','EDITOR')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeCategoryRepository categoryRepository;

    @Autowired
    public KnowledgeController(KnowledgeService knowledgeService,
                                KnowledgeCategoryRepository categoryRepository) {
        this.knowledgeService = knowledgeService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Map<String, Object> response = new LinkedHashMap<>();
        List<KnowledgeDocument> docs = knowledgeService.searchDocuments(query, PageRequest.of(page, size));
        response.put("content", docs);
        response.put("totalElements", docs.size());
        response.put("page", page);
        response.put("size", size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<KnowledgeCategory>> categories() {
        return ResponseEntity.ok(categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeDocument> get(@PathVariable String id) {
        KnowledgeDocument d = knowledgeService.getDocumentById(id);
        if (d == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(d);
    }

    @PostMapping
    public ResponseEntity<KnowledgeDocument> create(@RequestBody KnowledgeDocument doc) {
        return ResponseEntity.ok(knowledgeService.createDocument(doc));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeDocument> update(@PathVariable String id, @RequestBody KnowledgeDocument doc) {
        return ResponseEntity.ok(knowledgeService.updateDocument(id, doc));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        knowledgeService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<KnowledgeDocument> approve(@PathVariable String id, @RequestParam String approvedBy) {
        return ResponseEntity.ok(knowledgeService.approveDocument(id, approvedBy));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<KnowledgeDocument> reject(@PathVariable String id, @RequestParam String rejectedBy) {
        return ResponseEntity.ok(knowledgeService.rejectDocument(id, rejectedBy));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String docTitle = (title != null && !title.isBlank()) ? title : file.getOriginalFilename();
        String docCategory = (category != null && !category.isBlank()) ? category : "UPLOADED";
        Map<String, Object> result = knowledgeService.uploadAndProcess(docTitle, content, docCategory, "FILE_UPLOAD");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/text")
    public ResponseEntity<?> uploadText(@RequestBody Map<String, String> payload) {
        String title = payload.getOrDefault("title", "Untitled");
        String content = payload.getOrDefault("content", "");
        String category = payload.getOrDefault("category", "TEXT");
        Map<String, Object> result = knowledgeService.uploadAndProcess(title, content, category, "TEXT");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reindex")
    public ResponseEntity<?> reindex() {
        List<KnowledgeDocument> all = knowledgeService.getApprovedDocuments();
        int count = 0;
        for (KnowledgeDocument d : all) {
            knowledgeService.processAndIndex(d.getId());
            count++;
        }
        return ResponseEntity.ok(Map.of("success", true, "reindexed", count));
    }
}
