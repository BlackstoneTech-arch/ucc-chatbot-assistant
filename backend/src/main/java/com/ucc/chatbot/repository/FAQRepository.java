package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FAQRepository extends JpaRepository<FAQ, String> {
    List<FAQ> findByIsPublishedTrue();
    List<FAQ> findByCategory(String category);
    List<FAQ> findAllByOrderByPriorityDesc();
}
