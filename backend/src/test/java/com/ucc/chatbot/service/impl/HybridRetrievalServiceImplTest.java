package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class HybridRetrievalServiceImplTest {

    @Test
    void vectorSearchReturnsRankedResults() {
        KnowledgeDocumentRepository repo = Mockito.mock(KnowledgeDocumentRepository.class);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(1L);
        doc.setTitle("Admissions");
        doc.setContent("Applications open 1 June and close 30 September.");
        doc.setCategory("Admissions");
        doc.setIsActive(true);
        when(repo.findByIsActiveTrue()).thenReturn(List.of(doc));

        HybridRetrievalServiceImpl service = new HybridRetrievalServiceImpl(repo);
        List<KnowledgeDocument> results = service.vectorSearch("admissions application", 5);

        assertFalse(results.isEmpty());
        assertEquals(1L, results.get(0).getId());
    }

    @Test
    void bm25SearchReturnsEmptyForNoMatch() {
        KnowledgeDocumentRepository repo = Mockito.mock(KnowledgeDocumentRepository.class);
        when(repo.findByIsActiveTrue()).thenReturn(List.of());

        HybridRetrievalServiceImpl service = new HybridRetrievalServiceImpl(repo);
        List<KnowledgeDocument> results = service.bm25Search("zzzz-no-match", 5);

        assertTrue(results.isEmpty());
    }
}
