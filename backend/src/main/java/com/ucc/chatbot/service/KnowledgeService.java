package com.ucc.chatbot.service;

import com.ucc.chatbot.model.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface KnowledgeService {
    List<KnowledgeDocument> getAllActiveDocuments();
    Page<KnowledgeDocument> searchDocuments(String query, Pageable pageable);
    Optional<KnowledgeDocument> getDocumentById(String id);
    KnowledgeDocument createDocument(KnowledgeDocument document);
    KnowledgeDocument updateDocument(String id, KnowledgeDocument document);
    void deleteDocument(String id);
    List<KnowledgeDocument> getDocumentsByCategory(String category);
    List<KnowledgeDocument> getActiveDocumentsByCategory(String category);
    KnowledgeDocument uploadDocument(String title, String category, String content, String sourceType, String academicYear);
}
