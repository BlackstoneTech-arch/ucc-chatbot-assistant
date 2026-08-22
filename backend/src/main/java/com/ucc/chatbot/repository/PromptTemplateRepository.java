package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {
    List<PromptTemplate> findByIsActiveTrue();
    List<PromptTemplate> findByType(String type);
}
