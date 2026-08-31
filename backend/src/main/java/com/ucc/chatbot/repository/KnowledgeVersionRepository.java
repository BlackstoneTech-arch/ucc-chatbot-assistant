package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.KnowledgeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeVersionRepository extends JpaRepository<KnowledgeVersion, String> {
    List<KnowledgeVersion> findByDocumentIdOrderByVersionNumberDesc(String documentId);
}
