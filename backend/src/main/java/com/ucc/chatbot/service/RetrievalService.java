package com.ucc.chatbot.service;

import com.ucc.chatbot.model.FAQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface RetrievalService {
    List<FAQ> searchFAQs(String query);
    List<FAQ> getFAQsByCategory(String category);
    Page<FAQ> getAllFAQs(Pageable pageable);
    Optional<FAQ> getFAQById(String id);
    FAQ createFAQ(FAQ faq);
    FAQ updateFAQ(String id, FAQ faq);
    void deleteFAQ(String id);
}
