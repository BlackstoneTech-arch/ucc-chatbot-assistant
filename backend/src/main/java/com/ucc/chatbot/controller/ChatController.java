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

    @GetMapping("/welcome")
    public ResponseEntity<Map<String, Object>> welcome(@RequestParam(value = "lang", required = false) String lang) {
        String l = (lang == null || lang.isBlank()) ? "en" : lang.toLowerCase();
        boolean sw = "sw".equals(l);
        String message = sw
                ? "Habari! 👋 Karibu katika Kituo cha Kompyuta cha Chuo Kikuu cha Dar es Salaam (UCC). Mimi ni Msaidizi wako wa Huduma kwa Wateja wa UCC.\n\nHivi ndivyo ninavyoweza kukusaidia sasa hivi:\n• Programu na ada (DCIT, DBIT, CCIT, CBIT, kozi za kitaalamu)\n• Udaahili (dirisha wazi 1 Juni – 30 Septemba 2026, intake Septemba 2026)\n• Jinsi ya kuomba, vigezo vya kujiunga, maeneo\n• Mawasiliano na taarifa za kampasi\n\nAndika swali lako au chagua chaguo la haraka hapa chini."
                : "Hello! 👋 Welcome to the University of Dar es Salaam Computing Centre (UCC). I'm your UCC Customer Care Assistant.\n\nHere's what I can help you with right now:\n• Programmes and fees (DCIT, DBIT, CCIT, CBIT, professional courses)\n• Admissions (open 1 June – 30 Sept 2026, intake September 2026)\n• How to apply, entry requirements, locations\n• Contacts and campus info\n\nJust type your question or pick one of the quick options below.";
        List<Map<String, String>> quickReplies = sw
                ? List.of(
                        Map.of("label", "Programu zenu", "message", "Naomba kuona programu zenu"),
                        Map.of("label", "Ada ya DCIT", "message", "Ada ya DCIT ni ngapi?"),
                        Map.of("label", "Lini maombi yanafunguliwa?", "message", "Lini maombi yanafunguliwa na yanafungwa?"),
                        Map.of("label", "DCIT vs DBIT", "message", "DCIT na DBIT, ni ipi bora kwangu?"))
                : List.of(
                        Map.of("label", "Programmes", "message", "What programmes do you offer?"),
                        Map.of("label", "DCIT fees", "message", "How much is DCIT?"),
                        Map.of("label", "Admission dates", "message", "When do applications open and close?"),
                        Map.of("label", "DCIT vs DBIT", "message", "Which is better for me, DCIT or DBIT?"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("language", l);
        body.put("quickReplies", quickReplies);
        body.put("intakeOpen", "2026-06-01");
        body.put("intakeClose", "2026-09-30");
        body.put("intakeStart", "2026-09-01");
        body.put("applicationFee", "TZS 10,000");
        return ResponseEntity.ok(body);
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
