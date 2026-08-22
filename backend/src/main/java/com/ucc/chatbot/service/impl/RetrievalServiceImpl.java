package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.model.FAQ;
import com.ucc.chatbot.repository.FAQRepository;
import com.ucc.chatbot.service.RetrievalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RetrievalServiceImpl implements RetrievalService {

    private final FAQRepository faqRepository;

    public RetrievalServiceImpl(FAQRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Override
    public List<FAQ> searchFAQs(String query) {
        return faqRepository.findAll().stream()
                .filter(faq -> faq.getQuestion().toLowerCase().contains(query.toLowerCase()) ||
                        faq.getAnswer().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    @Override
    public List<FAQ> getFAQsByCategory(String category) {
        return faqRepository.findByCategoryAndStatus(category, "ACTIVE");
    }

    @Override
    public Page<FAQ> getAllFAQs(Pageable pageable) {
        return faqRepository.findAll(pageable);
    }

    @Override
    public Optional<FAQ> getFAQById(String id) {
        return faqRepository.findById(id);
    }

    @Override
    public FAQ createFAQ(FAQ faq) {
        return faqRepository.save(faq);
    }

    @Override
    public FAQ updateFAQ(String id, FAQ faq) {
        faq.setId(id);
        return faqRepository.save(faq);
    }

    @Override
    public void deleteFAQ(String id) {
        faqRepository.deleteById(id);
    }
}
