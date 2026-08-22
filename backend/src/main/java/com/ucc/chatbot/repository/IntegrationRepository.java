package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Integration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntegrationRepository extends JpaRepository<Integration, String> {
    List<Integration> findByStatus(String status);
}
