package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, String> {
    List<KnowledgeCategory> findByIsActiveTrue();
    Optional<KnowledgeCategory> findByName(String name);
}
