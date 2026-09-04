package com.ucc.chatbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
    private final DocumentCatalogService documentCatalogService;

    @Autowired
    public ChatController(ChatService chatService,
                           ConversationService conversationService,
                           MessageRepository messageRepository,
                           AIService aiService,
                           DocumentCatalogService documentCatalogService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.aiService = aiService;
        this.documentCatalogService = documentCatalogService;
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
                ? "Habari! 👋 Karibu katika Kituo cha Kompyuta cha Chuo Kikuu cha Dar es Salaam (UCC). Mimi ni Msaidizi wako wa Huduma kwa Wateja wa UCC.\n\nHivi ndivyo ninavyoweza kukusaidia sasa hivi:\n• Programu na ada (DCIT, DBIT, CCIT, CBIT, kozi za kitaalamu)\n• Udaahili wa Oktoba 2026/2027 (1 Juni – 30 Septemba, kuripoti Novemba 2026)\n• Kupakua brochure za programu na hati za kuomba\n• Jinsi ya kuomba, vigezo vya kujiunga, maeneo\n• Msaada wa kiufundi wa IT (ict@ucc.co.tz)\n• Mawasiliano na taarifa za kampasi\n\nAndika swali lako, uliza kuhusu 'pakua hati' kupata brochure, au chagua chaguo la haraka hapa chini."
                : "Hello! 👋 Welcome to the University of Dar es Salaam Computing Centre (UCC). I'm your UCC Customer Care Assistant.\n\nHere's what I can help you with right now:\n• Programmes and fees (DCIT, DBIT, CCIT, CBIT, professional courses)\n• October 2026/2027 Admissions (open 1 June – 30 Sept 2026, reporting November 2026)\n• Downloading programme brochures and the application pack from ucc.co.tz\n• How to apply, entry requirements, locations\n• ICT technical help (ict@ucc.co.tz)\n• Contacts and campus info\n\nJust type your question, ask 'download documents' to get brochures as links, or pick one of the quick options below.";
        List<Map<String, String>> quickReplies = sw
                ? List.of(
                        Map.of("label", "📥 Pakua Hati", "message", "Naomba pakua hati zote za kuomba"),
                        Map.of("label", "Programu zenu", "message", "Naomba kuona programu zenu"),
                        Map.of("label", "Ada ya DCIT", "message", "Ada ya DCIT ni ngapi?"),
                        Map.of("label", "Lini maombi yanafunguliwa?", "message", "Lini maombi yanafunguliwa na yanafungwa?"),
                        Map.of("label", "DCIT vs DBIT", "message", "DCIT na DBIT, ni ipi bora kwangu?"))
                : List.of(
                        Map.of("label", "📥 Download Documents", "message", "I need the application pack"),
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
        body.put("intakeStart", "2026-11-01");
        body.put("intakeLabel", "October 2026/2027");
        body.put("applicationFee", "TZS 15,000 (local) / TZS 30,000 (foreign)");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/documents")
    public ResponseEntity<JsonNode> listDocuments(@RequestParam(value = "category", required = false) String category,
                                                    @RequestParam(value = "q", required = false) String q) {
        JsonNode root = documentCatalogService.getAll();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("intake", root.path("intake"));
        out.put("categories", root.path("categories"));
        if (q != null && !q.isBlank()) {
            out.put("documents", documentCatalogService.search(q));
        } else if (category != null && !category.isBlank()) {
            out.put("documents", documentCatalogService.getDocumentsByCategory(category));
        } else {
            out.put("documents", root.path("documents"));
        }
        out.put("intakePack", root.path("intakePack"));
        return ResponseEntity.ok(documentCatalogService.getAll());
    }

    @GetMapping("/documents/pack")
    public ResponseEntity<Map<String, Object>> intakePack() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("intake", documentCatalogService.getAll().path("intake"));
        body.put("pack", documentCatalogService.getAll().path("intakePack"));
        body.put("documents", documentCatalogService.getIntakePack());
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
