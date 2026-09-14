package com.ucc.chatbot.service;

import com.ucc.chatbot.model.KnowledgeDocument;

import java.util.List;

/**
 * Hybrid retrieval: BM25 keyword search + vector semantic search.
 * Returns ranked results for the given query.
 */
public interface HybridRetrievalService {

    /**
     * Search the knowledge base using BM25 keyword matching.
     * Returns up to 10 documents ranked by relevance.
     */
    List<KnowledgeDocument> bm25Search(String query, int limit);

    /**
     * Search the knowledge base using vector/semantic similarity.
     * Returns up to 10 documents ranked by semantic similarity.
     */
    List<KnowledgeDocument> vectorSearch(String query, int limit);

    /**
     * Hybrid search: combines BM25 + vector results, deduplicates and reranks.
     * Returns up to 10 documents.
     */
    List<KnowledgeDocument> hybridSearch(String query, int limit);

    /**
     * Get the best matching document for a query, or null if no match.
     */
    KnowledgeDocument findBest(String query);
}