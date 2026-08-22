package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KnowledgeServiceImpl implements com.ucc.chatbot.service.KnowledgeService {

    private final KnowledgeDocumentRepository knowledgeRepository;

    public KnowledgeServiceImpl(KnowledgeDocumentRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public List<KnowledgeDocument> getAllActiveDocuments() {
        return knowledgeRepository.findByIsActiveTrue();
    }

    @Override
    public Page<KnowledgeDocument> searchDocuments(String query, Pageable pageable) {
        return knowledgeRepository.findAll(pageable);
    }

    @Override
    public Optional<KnowledgeDocument> getDocumentById(String id) {
        return knowledgeRepository.findById(id);
    }

    @Override
    public KnowledgeDocument createDocument(KnowledgeDocument document) {
        return knowledgeRepository.save(document);
    }

    @Override
    public KnowledgeDocument updateDocument(String id, KnowledgeDocument document) {
        document.setId(id);
        return knowledgeRepository.save(document);
    }

    @Override
    public void deleteDocument(String id) {
        knowledgeRepository.deleteById(id);
    }

    @Override
    public List<KnowledgeDocument> getDocumentsByCategory(String category) {
        return knowledgeRepository.findByCategoryAndIsActiveTrue(category);
    }

    @Override
    public List<KnowledgeDocument> getActiveDocumentsByCategory(String category) {
        return knowledgeRepository.findByCategoryAndIsActiveTrue(category);
    }
}
