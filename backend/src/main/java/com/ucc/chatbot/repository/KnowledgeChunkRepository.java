package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, String> {
    List<KnowledgeChunk> findByDocumentId(String documentId);
    void deleteByDocumentId(String documentId);
}
