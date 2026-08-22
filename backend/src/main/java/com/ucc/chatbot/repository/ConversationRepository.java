package com.ucc.chatbot.repository;

import com.ucc.chatbot.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Optional<Conversation> findBySessionId(String sessionId);
}
