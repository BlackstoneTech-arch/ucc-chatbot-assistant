package com.ucc.chatbot.controller;

import com.ucc.chatbot.config.JwtAuthFilter;
import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.model.User;
import com.ucc.chatbot.repository.ConversationRepository;
import com.ucc.chatbot.repository.UserRepository;
import com.ucc.chatbot.service.AuthService;
import com.ucc.chatbot.util.RoleNames;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String ACCESS_COOKIE = JwtAuthFilter.ACCESS_COOKIE;
    public static final String REFRESH_COOKIE = "ucc_refresh_token";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final boolean secureCookies;
    private final String sameSite;
    private final long refreshExpirationDays;

    public AuthController(AuthService authService,
                          UserRepository userRepository,
                          ConversationRepository conversationRepository,
                          @Value("${auth.cookie.secure:false}") boolean secureCookies,
                          @Value("${auth.cookie.same-site:Lax}") String sameSite,
                          @Value("${auth.refresh.expiration.days:7}") long refreshExpirationDays) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.secureCookies = secureCookies;
        this.sameSite = normalizeSameSite(sameSite);
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload,
                                   HttpServletResponse response) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(error("Email and password are required"));
        }
        Map<String, Object> result = authService.loginWithMap(email, password);
        if (!Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.status(401).body(sanitizedFailure(result));
        }
        result.put("redirect", routingFor(roleFrom(result)));
        return withSessionCookies(result, response);
    }

    @GetMapping("/csrf")
    public ResponseEntity<?> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody(required = false) Map<String, String> payload,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        String refreshToken = cookieValue(request, REFRESH_COOKIE);
        if ((refreshToken == null || refreshToken.isBlank()) && payload != null) {
            refreshToken = payload.get("refreshToken");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(error("Refresh token required"));
        }
        Map<String, Object> result = authService.refreshAccessToken(refreshToken);
        if (!Boolean.TRUE.equals(result.get("success"))) {
            clearSessionCookies(response);
            return ResponseEntity.status(401).body(sanitizedFailure(result));
        }
        return withSessionCookies(result, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> payload,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        String refreshToken = cookieValue(request, REFRESH_COOKIE);
        if ((refreshToken == null || refreshToken.isBlank()) && payload != null) {
            refreshToken = payload.get("refreshToken");
        }
        authService.logout(refreshToken);
        clearSessionCookies(response);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(error("Unauthorized"));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return ResponseEntity.status(401).body(error("Session is no longer active"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("role", RoleNames.normalize(user.getRole()));
        data.put("isActive", user.getIsActive());
        data.put("emailVerified", user.getEmailVerified());
        data.put("registrationNumber", user.getRegistrationNumber());
        data.put("createdAt", user.getCreatedAt());
        data.put("lastLogin", user.getLastLogin());
        return ResponseEntity.ok(Map.of("success", true, "user", data));
    }

    @PostMapping("/visitor-register")
    public ResponseEntity<?> registerVisitor(@RequestBody Map<String, String> payload) {
        return registrationResponse(authService.registerVisitor(payload));
    }

    @PostMapping("/student/register")
    public ResponseEntity<?> registerStudent(@RequestBody Map<String, String> payload) {
        return registrationResponse(authService.registerStudent(payload));
    }

    @PostMapping("/student/verify-email")
    public ResponseEntity<?> studentVerifyEmail(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(error("Email and password are required"));
        }
        return ResponseEntity.ok(authService.studentVerifyEmail(email, password));
    }

    @PostMapping("/student/verify-registration")
    public ResponseEntity<?> studentVerifyRegistration(@RequestBody Map<String, String> payload,
                                                       HttpServletResponse response) {
        Map<String, Object> result = authService.studentVerifyRegistration(payload);
        if (!Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.status(401).body(sanitizedFailure(result));
        }
        return withSessionCookies(result, response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(authService.forgotPassword(payload));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = authService.resetPassword(payload);
        return Boolean.TRUE.equals(result.get("success"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/link-conversation")
    public ResponseEntity<?> linkConversation(Authentication authentication,
                                               @RequestBody Map<String, String> payload) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).body(error("Unauthorized"));
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return ResponseEntity.status(401).body(error("Session is no longer active"));
        }
        String sessionId = payload.get("sessionId");
        if (sessionId == null || sessionId.isBlank() || sessionId.length() > 255) {
            return ResponseEntity.badRequest().body(error("A valid sessionId is required"));
        }
        Conversation conversation = conversationRepository.findBySessionId(sessionId).orElse(null);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setSessionId(sessionId);
            conversation.setUserId(user.getId());
            conversation.setIsActive(true);
            conversationRepository.save(conversation);
            return ResponseEntity.ok(Map.of("success", true,
                    "conversationId", conversation.getId(), "linked", true));
        }
        if (conversation.getUserId() != null && !user.getId().equals(conversation.getUserId())) {
            return ResponseEntity.status(403).body(error("Conversation does not belong to this account"));
        }
        boolean linked = conversation.getUserId() == null;
        if (linked) {
            conversation.setUserId(user.getId());
            conversationRepository.save(conversation);
        }
        return ResponseEntity.ok(Map.of("success", true,
                "conversationId", conversation.getId(), "linked", linked));
    }

    private ResponseEntity<?> registrationResponse(Map<String, Object> result) {
        return Boolean.TRUE.equals(result.get("success"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    private ResponseEntity<?> withSessionCookies(Map<String, Object> result, HttpServletResponse response) {
        Object accessToken = result.remove("token");
        Object refreshToken = result.remove("refreshToken");
        result.remove("loginResponse");
        if (accessToken != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(ACCESS_COOKIE,
                    String.valueOf(accessToken), 900).toString());
        }
        if (refreshToken != null) {
            long maxAge = Math.max(3600L, refreshExpirationDays * 24L * 60L * 60L);
            response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(REFRESH_COOKIE,
                    String.valueOf(refreshToken), maxAge).toString());
        }
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return ResponseEntity.ok(result);
    }

    private void clearSessionCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(ACCESS_COOKIE).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie(REFRESH_COOKIE).toString());
    }

    private ResponseCookie sessionCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookies)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)
                .build();
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "message", message);
    }

    private Map<String, Object> sanitizedFailure(Map<String, Object> result) {
        return error(result.get("message") instanceof String message ? message : "Invalid email or password");
    }

    private String roleFrom(Map<String, Object> result) {
        Object user = result.get("user");
        if (user instanceof Map<?, ?> map && map.get("role") != null) {
            return String.valueOf(map.get("role"));
        }
        return null;
    }

    private String routingFor(String role) {
        return RoleNames.isAdmin(role) ? "/admin/dashboard.html" : "/chat.html";
    }

    private String normalizeSameSite(String value) {
        if ("None".equalsIgnoreCase(value) || "Lax".equalsIgnoreCase(value)
                || "Strict".equalsIgnoreCase(value)) {
            if ("none".equalsIgnoreCase(value)) {
                return "None";
            }
            return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
        }
        return "Lax";
    }
}