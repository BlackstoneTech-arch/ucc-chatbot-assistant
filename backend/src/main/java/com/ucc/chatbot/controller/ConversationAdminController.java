package com.ucc.chatbot.controller;

import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/conversations")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','VIEWER')")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class ConversationAdminController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public ConversationAdminController(ConversationRepository conversationRepository,
                                        MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public ResponseEntity<List<Conversation>> list() {
        return ResponseEntity.ok(conversationRepository.findAllByOrderByStartedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        Conversation c = conversationRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversation", c);
        result.put("messages", messages);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        conversationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
