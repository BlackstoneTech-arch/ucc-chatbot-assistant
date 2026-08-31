package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.KnowledgeGap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeGapRepository extends JpaRepository<KnowledgeGap, String> {
    List<KnowledgeGap> findByResolvedFalseOrderByCountDesc();
    List<KnowledgeGap> findAllByOrderByCountDesc();
}
