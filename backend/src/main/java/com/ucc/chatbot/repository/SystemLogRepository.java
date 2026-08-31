package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, String> {
    List<SystemLog> findByLevelOrderByCreatedAtDesc(String level);
    List<SystemLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);
    List<SystemLog> findTop100ByOrderByCreatedAtDesc();
    long countByLevel(String level);
}
