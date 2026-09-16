package com.ucc.chatbot.controller;

import com.ucc.chatbot.service.AuthService;
import com.ucc.chatbot.service.JwtService;
import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.model.User;
import com.ucc.chatbot.repository.ConversationRepository;
import com.ucc.chatbot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://uccchatbot.netlify.app"})
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ConversationRepository conversationRepository;

    @Autowired
    public AuthController(AuthService authService, UserRepository userRepository, JwtService jwtService,
                             ConversationRepository conversationRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email and password required"));
        }
        return ResponseEntity.ok(authService.loginWithMap(email, password));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> payload) {
        String rt = payload.get("refreshToken");
        if (rt == null || rt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Refresh token required"));
        }
        return ResponseEntity.ok(authService.refreshAccessToken(rt));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> payload) {
        String token = payload.get("refreshToken");
        if (token != null) authService.logout(token);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        String token = auth.substring(7);
        try {
            String email = jwtService.extractEmail(token);
            User u = userRepository.findByEmail(email).orElse(null);
            if (u == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not found"));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", u.getId());
            data.put("email", u.getEmail());
            data.put("fullName", u.getFullName());
            data.put("role", u.getRole());
            data.put("isActive", u.getIsActive());
            data.put("phone", u.getPhone());
            data.put("studentNumber", u.getStudentNumber());
            return ResponseEntity.ok(Map.of("success", true, "user", data));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid token"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(authService.register(payload));
    }

    @PostMapping("/visitor-login")
    public ResponseEntity<?> visitorLogin(@RequestBody Map<String, String> payload) {
        String phone = payload.get("phone");
        String fullName = payload.get("fullName");
        return ResponseEntity.ok(authService.visitorLogin(phone, fullName));
    }

    @PostMapping("/student-register")
    public ResponseEntity<?> studentRegister(@RequestBody Map<String, String> payload) {
        payload.putIfAbsent("role", "USER");
        Map<String, Object> result = authService.register(payload);
        if (Boolean.TRUE.equals(result.get("success"))) {
            String userId = (String) result.get("id");
            if (userId != null) {
                userRepository.findById(userId).ifPresent(u -> {
                    if (payload.get("phone") != null) u.setPhone(payload.get("phone"));
                    if (payload.get("studentNumber") != null) u.setStudentNumber(payload.get("studentNumber"));
                    userRepository.save(u);
                });
            }
        }
        return ResponseEntity.ok(result);
    }


    @PostMapping("/link-conversation")
    public ResponseEntity<?> linkConversation(@RequestHeader(value = "Authorization", required = false) String auth,
                                              @RequestBody Map<String, String> payload) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        try {
            String email = jwtService.extractEmail(auth.substring(7));
            User u = userRepository.findByEmail(email).orElse(null);
            if (u == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not found"));
            String sessionId = payload.get("sessionId");
            if (sessionId == null || sessionId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "sessionId required"));
            }
            java.util.Optional<Conversation> opt = conversationRepository.findBySessionId(sessionId);
            if (opt.isEmpty()) {
                Conversation conv = new Conversation();
                conv.setSessionId(sessionId);
                conv.setUserId(u.getId());
                conv.setIsActive(true);
                conversationRepository.save(conv);
                return ResponseEntity.ok(Map.of("success", true, "conversationId", conv.getId(), "linked", true));
            }
            Conversation conv = opt.get();
            boolean alreadyLinked = u.getId().equals(conv.getId());
            if (!alreadyLinked) {
                conv.setUserId(u.getId());
                conversationRepository.save(conv);
            }
            return ResponseEntity.ok(Map.of("success", true, "conversationId", conv.getId(), "linked", !alreadyLinked));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid token"));
        }
    }
}
