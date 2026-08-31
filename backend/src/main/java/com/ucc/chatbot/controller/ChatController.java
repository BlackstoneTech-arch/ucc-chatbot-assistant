package com.ucc.chatbot.controller;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import com.ucc.chatbot.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final AIService aiService;

    @Autowired
    public ChatController(ChatService chatService,
                           ConversationService conversationService,
                           MessageRepository messageRepository,
                           AIService aiService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.aiService = aiService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.processMessage(request));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Map<String, Object>>> getHistory(@PathVariable String sessionId) {
        Optional<Conversation> convOpt = conversationService.getOrCreateConversation(sessionId, null);
        if (convOpt.isEmpty()) return ResponseEntity.ok(List.of());
        List<Message> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(convOpt.get().getId());
        List<Map<String, Object>> result = msgs.stream().map(m -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", m.getId());
            r.put("role", m.getRole());
            r.put("content", m.getContent());
            r.put("createdAt", m.getCreatedAt());
            return r;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> feedback(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.getOrDefault("sessionId", "anon");
        String messageId = (String) payload.getOrDefault("messageId", null);
        int rating = ((Number) payload.getOrDefault("rating", 5)).intValue();
        String comment = (String) payload.getOrDefault("comment", null);
        Feedback f = aiService.recordFeedback(sessionId, messageId, rating, comment);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", f.getId());
        return ResponseEntity.ok(result);
    }
}
