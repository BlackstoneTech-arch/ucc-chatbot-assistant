package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.LoginRequest;
import com.ucc.chatbot.dto.LoginResponse;
import com.ucc.chatbot.model.AILog;
import com.ucc.chatbot.model.AuditLog;
import com.ucc.chatbot.model.AuthenticationChallenge;
import com.ucc.chatbot.model.RefreshToken;
import com.ucc.chatbot.model.Role;
import com.ucc.chatbot.model.User;
import com.ucc.chatbot.repository.AILogRepository;
import com.ucc.chatbot.repository.AuditLogRepository;
import com.ucc.chatbot.repository.AuthenticationChallengeRepository;
import com.ucc.chatbot.repository.RefreshTokenRepository;
import com.ucc.chatbot.repository.RoleRepository;
import com.ucc.chatbot.repository.UserRepository;
import com.ucc.chatbot.service.AuthService;
import com.ucc.chatbot.service.JwtService;
import com.ucc.chatbot.util.RoleNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_LOGINS = 5;
    private static final long LOCK_MINUTES = 15;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthenticationChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AILogRepository aiLogRepository;
    private final long refreshExpirationDays;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                            UserRepository userRepository,
                            RoleRepository roleRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            AuditLogRepository auditLogRepository,
                            AuthenticationChallengeRepository challengeRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            AILogRepository aiLogRepository,
                            @Value("${auth.refresh.expiration.days:7}") long refreshExpirationDays) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.aiLogRepository = aiLogRepository;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Object value = loginWithMap(request.getEmail(), request.getPassword()).get("loginResponse");
        return value instanceof LoginResponse response ? response : null;
    }

    @Override
    @Transactional
    public Map<String, Object> loginWithMap(String rawEmail, String password) {
        Map<String, Object> result = new LinkedHashMap<>();
        String email = normalizeEmail(rawEmail);
        if (email == null || password == null || password.isBlank()) {
            return failure(result, "Invalid email or password");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && isLocked(user)) {
            logLoginAttempt(email, false);
            return failure(result, "Invalid email or password");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (Exception exception) {
            registerFailedLogin(user, email);
            logLoginAttempt(email, false);
            return failure(result, "Invalid email or password");
        }

        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            logLoginAttempt(email, false);
            return failure(result, "Invalid email or password");
        }

        user.setLastLogin(LocalDateTime.now());
        user.setLastSeenAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setRole(RoleNames.normalize(user.getRole()));
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole());
        RefreshToken refreshToken = createRefreshToken(user.getId());
        result.put("success", true);
        // AuthController consumes these values and places them in HttpOnly cookies.
        result.put("token", accessToken);
        result.put("refreshToken", refreshToken.getToken());
        result.put("user", publicUser(user));
        result.put("expiresIn", 900);
        result.put("loginResponse", LoginResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build());
        logLoginAttempt(email, true);
        audit(user.getId(), "LOGIN_SUCCESS", "user", user.getId(), null, null);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> refreshAccessToken(String rawRefreshToken) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return failure(result, "Invalid or expired refresh token");
        }
        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawRefreshToken).orElse(null);
        if (refreshToken == null || Boolean.TRUE.equals(refreshToken.getRevoked())
                || refreshToken.getExpiresAt() == null
                || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return failure(result, "Invalid or expired refresh token");
        }
        User user = userRepository.findById(refreshToken.getUserId()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive()) || isLocked(user)) {
            return failure(result, "User session is no longer active");
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        RefreshToken replacement = createRefreshToken(user.getId());
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole());
        result.put("success", true);
        result.put("token", accessToken);
        result.put("refreshToken", replacement.getToken());
        result.put("user", publicUser(user));
        result.put("expiresIn", 900);
        return result;
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public User createUser(String rawEmail, String password, String fullName, String role) {
        String email = normalizeEmail(rawEmail);
        validateRegistrationInput(email, password, fullName, null);
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        user.setRole(RoleNames.normalize(role));
        user.setIsActive(true);
        user.setFailedLoginCount(0);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        validatePassword(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void ensureDefaultAdmin(String rawEmail, String password, String fullName) {
        String email = normalizeEmail(rawEmail);
        if (email == null || password == null || password.isBlank() || fullName == null || fullName.isBlank()) {
            return;
        }
        ensureDefaultRoles();
        User admin = userRepository.findByEmail(email).orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setEmail(email);
            admin.setFullName(fullName.trim());
            admin.setRole(RoleNames.ADMIN);
            admin.setIsActive(true);
            admin.setFailedLoginCount(0);
        } else if (!RoleNames.isAdmin(admin.getRole())) {
            return;
        }
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(admin);
    }

    @Override
    @Transactional
    public Map<String, Object> registerVisitor(Map<String, String> payload) {
        return register(payload, RoleNames.VISITOR, null);
    }

    @Override
    @Transactional
    public Map<String, Object> registerStudent(Map<String, String> payload) {
        return register(payload, RoleNames.STUDENT, payload == null ? null : payload.get("registrationNumber"));
    }

    private Map<String, Object> register(Map<String, String> payload, String forcedRole, String registrationNumber) {
        Map<String, Object> result = new LinkedHashMap<>();
        String email = normalizeEmail(payload == null ? null : payload.get("email"));
        String password = payload == null ? null : payload.get("password");
        String fullName = payload == null ? null : payload.get("fullName");
        String confirm = payload == null ? null : payload.get("confirmPassword");
        String normalizedRegistration = registrationNumber == null ? null : registrationNumber.trim().toUpperCase(Locale.ROOT);
        try {
            validateRegistrationInput(email, password, fullName, normalizedRegistration);
            if (confirm == null || !confirm.equals(password)) {
                return failure(result, "Passwords do not match");
            }
            if (userRepository.existsByEmail(email)) {
                return failure(result, "An account with that email already exists");
            }
            if (RoleNames.STUDENT.equals(forcedRole)
                    && (normalizedRegistration == null || userRepository.existsByRegistrationNumber(normalizedRegistration))) {
                return failure(result, "Registration number is already in use or missing");
            }
        } catch (IllegalArgumentException exception) {
            return failure(result, exception.getMessage());
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName.trim());
        user.setRole(RoleNames.normalize(forcedRole));
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setFailedLoginCount(0);
        if (RoleNames.STUDENT.equals(user.getRole())) {
            user.setRegistrationNumber(normalizedRegistration);
            user.setStudentNumber(normalizedRegistration);
        }
        user = userRepository.save(user);
        audit(user.getId(), "ACCOUNT_CREATED", "user", user.getId(), null, null);
        result.put("success", true);
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("fullName", user.getFullName());
        result.put("role", user.getRole());
        if (user.getRegistrationNumber() != null) {
            result.put("registrationNumber", user.getRegistrationNumber());
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> studentVerifyEmail(String rawEmail, String password) {
        Map<String, Object> result = new LinkedHashMap<>();
        String email = normalizeEmail(rawEmail);
        if (email == null || password == null) {
            return failure(result, "Invalid email or password");
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !RoleNames.STUDENT.equals(RoleNames.normalize(user.getRole()))
                || !Boolean.TRUE.equals(user.getIsActive()) || isLocked(user)
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            logLoginAttempt(email, false);
            return failure(result, "Invalid email or password");
        }
        AuthenticationChallenge challenge = createStudentChallenge(user.getId());
        result.put("success", true);
        result.put("requiresRegistrationNumber", true);
        result.put("challengeToken", challenge.getToken());
        result.put("expiresIn", 300);
        result.put("email", user.getEmail());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> studentVerifyRegistration(Map<String, String> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        String challengeToken = payload == null ? null : payload.get("challengeToken");
        String registrationNumber = payload == null ? null : payload.get("registrationNumber");
        String password = payload == null ? null : payload.get("password");
        if (challengeToken == null || registrationNumber == null || password == null) {
            return failure(result, "Invalid registration number or password");
        }
        AuthenticationChallenge challenge = challengeRepository.findByToken(challengeToken).orElse(null);
        if (challenge == null || Boolean.TRUE.equals(challenge.getUsed())
                || challenge.getExpiresAt() == null || challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            return failure(result, "Invalid registration number or password");
        }
        User user = userRepository.findById(challenge.getUserId()).orElse(null);
        if (user == null || !RoleNames.STUDENT.equals(RoleNames.normalize(user.getRole()))
                || !Boolean.TRUE.equals(user.getIsActive()) || isLocked(user)) {
            return failure(result, "Invalid registration number or password");
        }
        String normalizedRegistration = registrationNumber.trim().toUpperCase(Locale.ROOT);
        if (!normalizedRegistration.equalsIgnoreCase(user.getRegistrationNumber())
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            challengeRepository.markUsed(challengeToken, LocalDateTime.now());
            registerFailedLogin(user, user.getEmail());
            return failure(result, "Invalid registration number or password");
        }

        challengeRepository.markUsed(challengeToken, LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setLastSeenAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole());
        RefreshToken refreshToken = createRefreshToken(user.getId());
        result.put("success", true);
        result.put("token", accessToken);
        result.put("refreshToken", refreshToken.getToken());
        result.put("user", publicUser(user));
        result.put("expiresIn", 900);
        result.put("redirect", "/chat.html");
        logLoginAttempt(user.getEmail(), true);
        audit(user.getId(), "LOGIN_SUCCESS", "user", user.getId(), null, null);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> forgotPassword(Map<String, String> payload) {
        return Map.of("success", true,
                "message", "If an account exists, password reset instructions have been sent.");
    }

    @Override
    @Transactional
    public Map<String, Object> resetPassword(Map<String, String> payload) {
        return Map.of("success", false,
                "message", "Password reset is not available. Please contact UCC support.");
    }

    @Override
    @Transactional
    public AuthenticationChallenge createStudentChallenge(String userId) {
        AuthenticationChallenge challenge = new AuthenticationChallenge();
        challenge.setUserId(userId);
        challenge.setToken(UUID.randomUUID() + "." + UUID.randomUUID());
        challenge.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        challenge.setUsed(false);
        return challengeRepository.save(challenge);
    }

    private RefreshToken createRefreshToken(String userId) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID() + "." + UUID.randomUUID());
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        token.setCreatedAt(LocalDateTime.now());
        token.setRevoked(false);
        return refreshTokenRepository.save(token);
    }

    private Map<String, Object> publicUser(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("role", RoleNames.normalize(user.getRole()));
        data.put("isActive", user.getIsActive());
        data.put("emailVerified", user.getEmailVerified());
        if (user.getRegistrationNumber() != null) {
            data.put("registrationNumber", user.getRegistrationNumber());
        }
        return data;
    }

    private Map<String, Object> failure(Map<String, Object> result, String message) {
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    private void validateRegistrationInput(String email, String password, String fullName, String registrationNumber) {
        if (email == null || !email.matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 255) {
            throw new IllegalArgumentException("Full name is required");
        }
        validatePassword(password);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Password must be 8-128 characters and contain at least one letter and one number");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void registerFailedLogin(User user, String email) {
        if (user == null) {
            return;
        }
        int failures = user.getFailedLoginCount() == null ? 1 : user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(failures);
        if (failures >= MAX_FAILED_LOGINS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setFailedLoginCount(0);
        }
        userRepository.save(user);
    }

    private void ensureDefaultRoles() {
        if (roleRepository.count() > 0) {
            return;
        }
        createRole(RoleNames.ADMIN, "Full system administrator");
        createRole(RoleNames.SUPER_ADMIN, "Super administrator");
        createRole(RoleNames.STAFF, "UCC staff member");
        createRole(RoleNames.EDITOR, "Knowledge editor");
        createRole(RoleNames.VIEWER, "Read-only administrator");
        createRole(RoleNames.STUDENT, "Student");
        createRole(RoleNames.VISITOR, "Visitor");
    }

    private void createRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }

    private void audit(String userId, String action, String resourceType, String resourceId,
                        String oldValues, String newValues) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setResourceType(resourceType);
            log.setResourceId(resourceId);
            log.setOldValues(oldValues);
            log.setNewValues(newValues);
            auditLogRepository.save(log);
        } catch (Exception ignored) {
            // Audit logging must not turn a successful login into a server error.
        }
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
        } catch (Exception ignored) {
            // Best-effort security telemetry.
        }
    }
}