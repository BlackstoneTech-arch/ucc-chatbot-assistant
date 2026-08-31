package com.ucc.chatbot.controller;

import com.ucc.chatbot.service.AuthService;
import com.ucc.chatbot.service.JwtService;
import com.ucc.chatbot.model.User;
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

    @Autowired
    public AuthController(AuthService authService, UserRepository userRepository, JwtService jwtService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
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
        String token = payload.get("refreshToken");
        if (token == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Refresh token required"));
        return ResponseEntity.ok(authService.refreshAccessToken(token));
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
            return ResponseEntity.ok(Map.of("success", true, "user", data));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid token"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(authService.register(payload));
    }
}
