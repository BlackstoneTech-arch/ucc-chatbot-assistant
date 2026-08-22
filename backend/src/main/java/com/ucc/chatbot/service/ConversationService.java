package com.ucc.chatbot.service;

import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.model.Message;
import java.util.List;
import java.util.Optional;

public interface ConversationService {
    Optional<Conversation> getOrCreateConversation(String sessionId, String userId);
    Message saveMessage(Conversation conversation, String sender, String content);
    List<Message> getMessages(String conversationId);
    List<Conversation> getAllConversations();
    Optional<Conversation> getConversationById(String id);
    void updateContext(String conversationId, String programme, String concept, String intent);
}
