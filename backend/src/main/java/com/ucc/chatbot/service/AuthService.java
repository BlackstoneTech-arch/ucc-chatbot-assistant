package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.LoginRequest;
import com.ucc.chatbot.dto.LoginResponse;
import com.ucc.chatbot.model.User;
import com.ucc.chatbot.model.Role;
import com.ucc.chatbot.model.RefreshToken;
import com.ucc.chatbot.model.AILog;
import com.ucc.chatbot.repository.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    Map<String, Object> loginWithMap(String email, String password);
    Map<String, Object> refreshAccessToken(String refreshToken);
    void logout(String refreshToken);
    User createUser(String email, String password, String fullName, String role);
    User changePassword(String userId, String currentPassword, String newPassword);
    void ensureDefaultAdmin(String email, String password, String fullName);
    void logout(String token);
    Map<String, Object> register(Map<String, String> payload);
}
