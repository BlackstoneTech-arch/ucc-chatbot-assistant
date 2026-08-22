package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    List<KnowledgeDocument> findByIsActiveTrue();
    List<KnowledgeDocument> findByCategoryAndIsActiveTrue(String category);

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.isActive = true AND " +
           "(LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(k.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(k.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<KnowledgeDocument> searchActive(@Param("query") String query);
}
