package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    List<KnowledgeDocument> findByIsActiveTrue();
    List<KnowledgeDocument> findByCategoryAndIsActiveTrue(String category);
}
