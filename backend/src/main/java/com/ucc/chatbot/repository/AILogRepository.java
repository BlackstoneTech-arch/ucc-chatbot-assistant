package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.AILog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AILogRepository extends JpaRepository<AILog, String> {
    List<AILog> findTop100ByOrderByCreatedAtDesc();
    List<AILog> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    long countByCreatedAtAfter(java.time.LocalDateTime after);
}
