package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.AIPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AIPromptRepository extends JpaRepository<AIPrompt, String> {
    List<AIPrompt> findByIsActiveTrue();
    List<AIPrompt> findByCategory(String category);
}
