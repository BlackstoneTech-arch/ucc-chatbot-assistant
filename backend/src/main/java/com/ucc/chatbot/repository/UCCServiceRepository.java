package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.UCCService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UCCServiceRepository extends JpaRepository<UCCService, String> {
    List<UCCService> findByIsActiveTrueOrderByDisplayOrderAsc();
}
