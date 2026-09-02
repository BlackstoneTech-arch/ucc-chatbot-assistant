package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
    List<KnowledgeDocument> findByIsActiveTrue();
    List<KnowledgeDocument> findByApprovalStatusAndIsActiveTrue(String status);
    List<KnowledgeDocument> findByCategory(String category);
    Page<KnowledgeDocument> findAll(Pageable pageable);

    @Query(value = "SELECT * FROM knowledge_documents WHERE is_active = true AND approval_status = 'APPROVED' AND " +
           "(LOWER(title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(category) LIKE LOWER(CONCAT('%', :query, '%')))", nativeQuery = true)
    List<KnowledgeDocument> searchApproved(@Param("query") String query);

    @Query("SELECT k FROM KnowledgeDocument k WHERE k.isActive = true AND k.approvalStatus = 'APPROVED' AND k.category = :category")
    List<KnowledgeDocument> findByCategoryApproved(@Param("category") String category);

    long countByApprovalStatusAndIsActiveTrue(String status);
}
