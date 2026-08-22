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
                return existing;
            }
        }

        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId != null ? sessionId : java.util.UUID.randomUUID().toString());
        conversation.setIsActive(true);
        return Optional.of(conversationRepository.save(conversation));
    }

    @Override
    public Message saveMessage(Conversation conversation, String sender, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
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
}
