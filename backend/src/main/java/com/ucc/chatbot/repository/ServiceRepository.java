package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, String> {
    List<Service> findByStatus(String status);
    List<Service> findByCategoryAndStatus(String category, String status);
}
