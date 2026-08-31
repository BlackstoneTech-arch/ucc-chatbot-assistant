package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.model.KnowledgeChunk;
import com.ucc.chatbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeDocumentRepository knowledgeRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeVersionRepository versionRepository;
    private final AILogRepository aiLogRepository;

    @Autowired
    public KnowledgeServiceImpl(KnowledgeDocumentRepository knowledgeRepository,
                                 KnowledgeChunkRepository chunkRepository,
                                 KnowledgeVersionRepository versionRepository,
                                 AILogRepository aiLogRepository) {
        this.knowledgeRepository = knowledgeRepository;
        this.chunkRepository = chunkRepository;
        this.versionRepository = versionRepository;
        this.aiLogRepository = aiLogRepository;
    }

    @Override
    public List<KnowledgeDocument> getAllActiveDocuments() {
        return knowledgeRepository.findByIsActiveTrue();
    }

    @Override
    public List<KnowledgeDocument> getApprovedDocuments() {
        return knowledgeRepository.findByApprovalStatusAndIsActiveTrue("APPROVED");
    }

    @Override
    public KnowledgeDocument getDocumentById(String id) {
        return knowledgeRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public KnowledgeDocument createDocument(KnowledgeDocument document) {
        if (document.getContentHash() == null) {
            document.setContentHash(String.valueOf(document.getContent().hashCode()));
        }
        if (document.getApprovalStatus() == null) document.setApprovalStatus("PENDING");
        return knowledgeRepository.save(document);
    }

    @Override
    @Transactional
    public KnowledgeDocument updateDocument(String id, KnowledgeDocument document) {
        KnowledgeDocument existing = knowledgeRepository.findById(id).orElse(null);
        if (existing == null) return null;
        saveVersion(existing, "Update");
        existing.setTitle(document.getTitle());
        existing.setDescription(document.getDescription());
        existing.setContent(document.getContent());
        existing.setCategory(document.getCategory());
        existing.setCategoryId(document.getCategoryId());
        existing.setSourceUrl(document.getSourceUrl());
        existing.setSourceType(document.getSourceType());
        existing.setAcademicYear(document.getAcademicYear());
        existing.setLanguage(document.getLanguage());
        existing.setEffectiveDate(document.getEffectiveDate());
        existing.setExpiryDate(document.getExpiryDate());
        existing.setApprovalStatus("PENDING");
        existing.setContentHash(String.valueOf(document.getContent().hashCode()));
        existing.setIsIndexed(false);
        existing.setVersion(existing.getVersion() + 1);
        return knowledgeRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteDocument(String id) {
        knowledgeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public KnowledgeDocument approveDocument(String id, String approvedBy) {
        KnowledgeDocument doc = knowledgeRepository.findById(id).orElse(null);
        if (doc == null) return null;
        doc.setApprovalStatus("APPROVED");
        doc.setApprovedBy(approvedBy);
        doc.setApprovedAt(java.time.LocalDateTime.now());
        doc.setIsActive(true);
        KnowledgeDocument saved = knowledgeRepository.save(doc);
        processAndIndex(id);
        return saved;
    }

    @Override
    @Transactional
    public KnowledgeDocument rejectDocument(String id, String rejectedBy) {
        KnowledgeDocument doc = knowledgeRepository.findById(id).orElse(null);
        if (doc == null) return null;
        doc.setApprovalStatus("REJECTED");
        return knowledgeRepository.save(doc);
    }

    @Override
    public List<KnowledgeDocument> searchApproved(String query) {
        if (query == null || query.isBlank()) return List.of();
        return knowledgeRepository.searchApproved(query.trim());
    }

    @Override
    public List<Map<String, Object>> retrieveAndRank(String query, String category, int topK) {
        List<KnowledgeDocument> docs;
        if (category != null && !category.isBlank()) {
            docs = knowledgeRepository.findByCategoryApproved(category);
        } else {
            docs = searchApproved(query);
        }
        String lowerQ = query == null ? "" : query.toLowerCase();
        return docs.stream()
            .map(d -> {
                double score = scoreDocument(d, lowerQ);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", d.getId());
                result.put("title", d.getTitle());
                result.put("content", truncate(d.getContent(), 600));
                result.put("category", d.getCategory());
                result.put("sourceUrl", d.getSourceUrl());
                result.put("score", score);
                return result;
            })
            .sorted((a, b) -> Double.compare((double) b.get("score"), (double) a.get("score")))
            .limit(topK)
            .collect(Collectors.toList());
    }

    private double scoreDocument(KnowledgeDocument d, String lowerQuery) {
        if (lowerQuery.isEmpty()) return 0.5;
        String title = d.getTitle() == null ? "" : d.getTitle().toLowerCase();
        String content = d.getContent() == null ? "" : d.getContent().toLowerCase();
        String category = d.getCategory() == null ? "" : d.getCategory().toLowerCase();
        double score = 0;
        String[] tokens = lowerQuery.split("\\s+");
        for (String token : tokens) {
            if (token.length() < 2) continue;
            if (title.contains(token)) score += 3.0;
            if (content.contains(token)) score += 1.0;
            if (category.contains(token)) score += 2.0;
        }
        return Math.min(score / Math.max(tokens.length, 1), 1.0);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    @Override
    public List<KnowledgeDocument> searchDocuments(String query, org.springframework.data.domain.Pageable pageable) {
        if (query == null || query.isBlank()) {
            return knowledgeRepository.findAll(pageable).getContent();
        }
        List<KnowledgeDocument> results = knowledgeRepository.searchApproved(query.trim());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        if (start >= results.size()) return List.of();
        return results.subList(start, end);
    }

    @Override
    @Transactional
    public String processAndIndex(String documentId) {
        KnowledgeDocument doc = knowledgeRepository.findById(documentId).orElse(null);
        if (doc == null) return "Document not found";
        chunkRepository.deleteByDocumentId(documentId);
        List<String> chunks = splitIntoChunks(doc.getContent(), 500);
        int idx = 0;
        for (String chunk : chunks) {
            KnowledgeChunk kc = new KnowledgeChunk();
            kc.setDocumentId(documentId);
            kc.setChunkIndex(idx++);
            kc.setChunkText(chunk);
            kc.setTokenCount(chunk.split("\\s+").length);
            chunkRepository.save(kc);
        }
        doc.setIsIndexed(true);
        doc.setIndexedAt(java.time.LocalDateTime.now());
        knowledgeRepository.save(doc);
        return "Indexed " + chunks.size() + " chunks";
    }

    @Override
    @Transactional
    public Map<String, Object> uploadAndProcess(String title, String content, String category, String sourceType) {
        Map<String, Object> result = new LinkedHashMap<>();
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setCategory(category);
        doc.setSourceType(sourceType);
        doc.setLanguage(detectLanguage(content));
        doc.setIsActive(true);
        doc.setApprovalStatus("APPROVED");
        doc.setContentHash(String.valueOf(content.hashCode()));
        KnowledgeDocument saved = knowledgeRepository.save(doc);
        String indexStatus = processAndIndex(saved.getId());
        result.put("id", saved.getId());
        result.put("title", saved.getTitle());
        result.put("status", "created");
        result.put("indexStatus", indexStatus);
        result.put("language", saved.getLanguage());
        return result;
    }

    @Override
    public List<String> splitIntoChunks(String content, int maxChunkSize) {
        if (content == null || content.isEmpty()) return List.of();
        List<String> chunks = new ArrayList<>();
        String[] sentences = content.split("(?<=[.!?\\n])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > maxChunkSize && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(sentence).append(" ");
        }
        if (current.length() > 0) chunks.add(current.toString().trim());
        return chunks;
    }

    private void saveVersion(KnowledgeDocument doc, String note) {
        KnowledgeVersion v = new KnowledgeVersion();
        v.setDocumentId(doc.getId());
        v.setVersionNumber(doc.getVersion());
        v.setTitle(doc.getTitle());
        v.setContent(doc.getContent());
        v.setContentHash(doc.getContentHash());
        v.setChangeNote(note);
        versionRepository.save(v);
    }

    private String detectLanguage(String text) {
        if (text == null) return "en";
        String lower = text.toLowerCase();
        String[] swMarkers = {"habari", "kozi", "ada", "programu", "maombi", "usajili", "nataka", "ni", "ya", "wa", "kwa", "za"};
        int count = 0;
        for (String m : swMarkers) if (lower.contains(m)) count++;
        return count >= 2 ? "sw" : "en";
    }
}
