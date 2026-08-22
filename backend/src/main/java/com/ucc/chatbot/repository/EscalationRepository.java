package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, String> {
    List<Escalation> findByStatusOrderByCreatedAtDesc(String status);
}
