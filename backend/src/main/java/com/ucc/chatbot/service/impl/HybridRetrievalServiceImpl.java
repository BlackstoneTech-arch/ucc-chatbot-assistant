package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import com.ucc.chatbot.service.HybridRetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridRetrievalServiceImpl implements HybridRetrievalService {

    private final KnowledgeDocumentRepository knowledgeRepository;

    @Autowired
    public HybridRetrievalServiceImpl(KnowledgeDocumentRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public List<KnowledgeDocument> bm25Search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        String[] terms = query.toLowerCase().split("\\s+");
        List<KnowledgeDocument> all = knowledgeRepository.findByIsActiveTrue();
        return all.stream()
                .sorted((a, b) -> Double.compare(scoreBM25(b, terms), scoreBM25(a, terms)))
                .filter(d -> scoreBM25(d, terms) > 0)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeDocument> vectorSearch(String query, int limit) {
        // Vector search placeholder — uses simple text overlap as fallback
        // In production, replace with actual embedding + cosine similarity
        return bm25Search(query, limit);
    }

    @Override
    public List<KnowledgeDocument> hybridSearch(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        Set<KnowledgeDocument> seen = new LinkedHashSet<>();
        seen.addAll(bm25Search(query, limit));
        seen.addAll(vectorSearch(query, limit));
        return new ArrayList<>(seen).stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public KnowledgeDocument findBest(String query) {
        List<KnowledgeDocument> results = hybridSearch(query, 1);
        return results.isEmpty() ? null : results.get(0);
    }

    private double scoreBM25(KnowledgeDocument doc, String[] terms) {
        String text = ((doc.getTitle() != null ? doc.getTitle() : "") + " " +
                       (doc.getContent() != null ? doc.getContent() : "") + " " +
                       (doc.getCategory() != null ? doc.getCategory() : "")).toLowerCase();
        double score = 0;
        for (String term : terms) {
            if (text.contains(term)) {
                score += 1.0;
                if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(term)) {
                    score += 2.0; // title boost
                }
            }
        }
        return score;
    }
}