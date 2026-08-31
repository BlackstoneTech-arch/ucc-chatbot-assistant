package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.LoginRequest;
import com.ucc.chatbot.dto.LoginResponse;
import com.ucc.chatbot.model.User;
import com.ucc.chatbot.model.Role;
import com.ucc.chatbot.model.RefreshToken;
import com.ucc.chatbot.model.AILog;
import com.ucc.chatbot.repository.*;
import com.ucc.chatbot.service.AuthService;
import com.ucc.chatbot.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AILogRepository aiLogRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                            UserRepository userRepository,
                            RoleRepository roleRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            AILogRepository aiLogRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.aiLogRepository = aiLogRepository;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        return loginWithMap(request.getEmail(), request.getPassword()).get("loginResponse") instanceof LoginResponse lr ? lr : null;
    }

    @Override
    @Transactional
    public Map<String, Object> loginWithMap(String email, String password) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Invalid credentials");
            logLoginAttempt(email, false);
            return result;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            result.put("success", false);
            result.put("message", "User not found");
            return result;
        }
        if (!user.getIsActive()) {
            result.put("success", false);
            result.put("message", "Account disabled");
            return result;
        }
        user.setLastLogin(LocalDateTime.now());
        user.setFailedLoginCount(0);
        userRepository.save(user);
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole());
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setToken(UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString());
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        rt.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(rt);
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("fullName", user.getFullName());
        userData.put("role", user.getRole());
        userData.put("isActive", user.getIsActive());
        LoginResponse lr = LoginResponse.builder()
            .token(accessToken)
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .build();
        result.put("success", true);
        result.put("token", accessToken);
        result.put("refreshToken", rt.getToken());
        result.put("user", userData);
        result.put("expiresIn", 86400);
        result.put("loginResponse", lr);
        logLoginAttempt(email, true);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        Map<String, Object> result = new LinkedHashMap<>();
        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken).orElse(null);
        if (rt == null || rt.getRevoked() || rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            result.put("success", false);
            result.put("message", "Invalid or expired refresh token");
            return result;
        }
        User user = userRepository.findById(rt.getUserId()).orElse(null);
        if (user == null || !user.getIsActive()) {
            result.put("success", false);
            result.put("message", "User inactive");
            return result;
        }
        String newToken = jwtService.generateToken(user.getEmail(), user.getRole());
        result.put("success", true);
        result.put("token", newToken);
        result.put("expiresIn", 86400);
        return result;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null) return;
        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional
    public User createUser(String email, String password, String fullName, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setFullName(fullName);
        u.setRole(role == null ? "USER" : role.toUpperCase());
        u.setIsActive(true);
        u.setFailedLoginCount(0);
        return userRepository.save(u);
    }

    @Override
    @Transactional
    public User changePassword(String userId, String currentPassword, String newPassword) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) throw new IllegalArgumentException("User not found");
        if (!passwordEncoder.matches(currentPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("Current password incorrect");
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(u);
    }

    @Override
    @Transactional
    public void ensureDefaultAdmin(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) return;
        ensureDefaultRoles();
        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName(fullName);
        admin.setRole("ADMIN");
        admin.setIsActive(true);
        userRepository.save(admin);
    }

    @Override
    public Map<String, Object> register(Map<String, String> payload) {
        try {
            User u = createUser(
                payload.get("email"),
                payload.get("password"),
                payload.get("fullName"),
                payload.getOrDefault("role", "USER")
            );
            return Map.of("success", true, "id", u.getId());
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private void ensureDefaultRoles() {
        if (roleRepository.count() == 0) {
            createRole("ADMIN", "Full system administrator");
            createRole("STAFF", "UCC staff member");
            createRole("EDITOR", "Can manage knowledge base content");
            createRole("VIEWER", "Read-only access");
            createRole("USER", "End user / student");
        }
    }

    private void createRole(String name, String description) {
        Role r = new Role();
        r.setName(name);
        r.setDescription(description);
        roleRepository.save(r);
    }

    private void logLoginAttempt(String email, boolean success) {
        try {
            AILog log = new AILog();
            log.setType(success ? "LOGIN_SUCCESS" : "LOGIN_FAILED");
            log.setAction("AUTH");
            log.setUserEmail(email);
            log.setStatus(success ? "SUCCESS" : "FAILED");
            log.setMessage(success ? "User logged in" : "Failed login attempt");
            log.setCreatedAt(LocalDateTime.now());
            aiLogRepository.save(log);
        } catch (Exception e) {
            // best-effort
        }
    }
}
