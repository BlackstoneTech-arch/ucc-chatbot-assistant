package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FAQRepository extends JpaRepository<FAQ, String> {
    List<FAQ> findByStatusAndIsActiveTrue(String status);
    List<FAQ> findByCategoryAndStatus(String category, String status);
}
