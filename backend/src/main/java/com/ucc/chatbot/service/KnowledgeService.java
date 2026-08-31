package com.ucc.chatbot.service;

import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.model.KnowledgeChunk;
import java.util.List;
import java.util.Map;

public interface KnowledgeService {
    List<KnowledgeDocument> getAllActiveDocuments();
    List<KnowledgeDocument> getApprovedDocuments();
    KnowledgeDocument getDocumentById(String id);
    KnowledgeDocument createDocument(KnowledgeDocument document);
    KnowledgeDocument updateDocument(String id, KnowledgeDocument document);
    void deleteDocument(String id);
    KnowledgeDocument approveDocument(String id, String approvedBy);
    KnowledgeDocument rejectDocument(String id, String rejectedBy);
    List<KnowledgeDocument> searchApproved(String query);
    List<Map<String, Object>> retrieveAndRank(String query, String category, int topK);
    List<KnowledgeDocument> searchDocuments(String query, org.springframework.data.domain.Pageable pageable);
    String processAndIndex(String documentId);
    Map<String, Object> uploadAndProcess(String title, String content, String category, String sourceType);
    List<String> splitIntoChunks(String content, int maxChunkSize);
}
