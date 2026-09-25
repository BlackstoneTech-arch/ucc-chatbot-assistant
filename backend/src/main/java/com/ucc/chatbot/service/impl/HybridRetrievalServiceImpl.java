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
        if (query == null || query.isBlank()) return List.of();
        List<KnowledgeDocument> corpus = knowledgeRepository.findByIsActiveTrue();
        if (corpus.isEmpty()) return List.of();

        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty()) return List.of();

        List<KnowledgeDocument> candidates = corpus.stream()
                .filter(doc -> {
                    String text = docTitleContent(doc).toLowerCase();
                    return queryTerms.stream().anyMatch(text::contains);
                })
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return List.of();

        Map<String, Long> docFreq = new HashMap<>();
        for (KnowledgeDocument doc : candidates) {
            Set<String> terms = new HashSet<>(tokenize(docTitleContent(doc)));
            for (String t : terms) {
                docFreq.merge(t, 1L, Long::sum);
            }
        }
        int n = candidates.size();
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Long> e : docFreq.entrySet()) {
            idf.put(e.getKey(), Math.log((double) (n + 1) / (e.getValue() + 1)) + 1.0);
        }

        Map<String, Long> queryTf = new HashMap<>();
        for (String t : queryTerms) queryTf.merge(t, 1L, Long::sum);
        double queryNorm = 0;
        Map<String, Double> queryVec = new HashMap<>();
        for (Map.Entry<String, Long> e : queryTf.entrySet()) {
            double w = (e.getValue() + 1) * idf.getOrDefault(e.getKey(), 0.0);
            queryVec.put(e.getKey(), w);
            queryNorm += w * w;
        }
        queryNorm = Math.sqrt(queryNorm);
        if (queryNorm == 0) return List.of();

        List<ScoredDoc> scored = new ArrayList<>();
        for (KnowledgeDocument doc : candidates) {
            Map<String, Long> docTf = new HashMap<>();
            for (String t : tokenize(docTitleContent(doc))) docTf.merge(t, 1L, Long::sum);
            double dot = 0;
            double docNorm = 0;
            for (Map.Entry<String, Long> e : docTf.entrySet()) {
                double w = (e.getValue() + 1) * idf.getOrDefault(e.getKey(), 0.0);
                dot += w * queryVec.getOrDefault(e.getKey(), 0.0);
                docNorm += w * w;
            }
            double sim = (docNorm == 0) ? 0 : dot / (queryNorm * Math.sqrt(docNorm));
            if (sim > 0) scored.add(new ScoredDoc(doc, sim));
        }
        return scored.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(sd -> sd.doc)
                .collect(Collectors.toList());
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

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(s -> s.length() > 1)
                .collect(Collectors.toList());
    }

    private String docTitleContent(KnowledgeDocument doc) {
        StringBuilder sb = new StringBuilder();
        if (doc.getTitle() != null) sb.append(doc.getTitle()).append(' ');
        if (doc.getContent() != null) sb.append(doc.getContent()).append(' ');
        if (doc.getCategory() != null) sb.append(doc.getCategory()).append(' ');
        return sb.toString();
    }

    private static class ScoredDoc {
        final KnowledgeDocument doc;
        final double score;
        ScoredDoc(KnowledgeDocument doc, double score) {
            this.doc = doc;
            this.score = score;
        }
    }
}