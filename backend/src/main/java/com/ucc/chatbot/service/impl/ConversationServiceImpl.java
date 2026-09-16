package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.model.Message;
import com.ucc.chatbot.model.User;
import com.ucc.chatbot.repository.ConversationRepository;
import com.ucc.chatbot.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationServiceImpl implements com.ucc.chatbot.service.ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public Optional<Conversation> getOrCreateConversation(String sessionId, String userId) {
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<Conversation> existing = conversationRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                Conversation c = existing.get();
                // If this conversation is anonymous but now we have a user, link it.
                if ((c.getUserId() == null || c.getUserId().isBlank()) && userId != null && !userId.isBlank()) {
                    c.setUserId(userId);
                    conversationRepository.save(c);
                }
                return Optional.of(c);
            }
        }

        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId != null ? sessionId : java.util.UUID.randomUUID().toString());
        conversation.setUserId(userId != null && !userId.isBlank() ? userId : null);
        conversation.setIsActive(true);
        return Optional.of(conversationRepository.save(conversation));
    }

    @Override
    public Message saveMessage(Conversation conversation, String sender, String content) {
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setRole(sender);
        message.setContent(content);
        return messageRepository.save(message);
    }

    @Override
    public List<Message> getMessages(String conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    public List<Conversation> getAllConversations() {
        return conversationRepository.findAll();
    }

    @Override
    public Optional<Conversation> getConversationById(String id) {
        return conversationRepository.findById(id);
    }

    @Override
    public void updateContext(String conversationId, String programme, String concept, String intent) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null) {
            if (programme != null) conversation.setLastProgramme(programme);
            if (concept != null) conversation.setLastConcept(concept);
            if (intent != null) conversation.setLastIntent(intent);
            conversationRepository.save(conversation);
        }
    }
}
