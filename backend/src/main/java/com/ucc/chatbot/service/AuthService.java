package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.LoginRequest;
import com.ucc.chatbot.dto.LoginResponse;
import com.ucc.chatbot.model.AuthenticationChallenge;
import com.ucc.chatbot.model.User;
import java.util.Map;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    Map<String, Object> loginWithMap(String email, String password);
    Map<String, Object> refreshAccessToken(String refreshToken);
    void logout(String refreshToken);
    Map<String, Object> registerVisitor(Map<String, String> payload);
    Map<String, Object> registerStudent(Map<String, String> payload);
    Map<String, Object> studentVerifyEmail(String email, String password);
    Map<String, Object> studentVerifyRegistration(Map<String, String> payload);
    Map<String, Object> forgotPassword(Map<String, String> payload);
    Map<String, Object> resetPassword(Map<String, String> payload);
    User createUser(String email, String password, String fullName, String role);
    User changePassword(String userId, String currentPassword, String newPassword);
    void ensureDefaultAdmin(String email, String password, String fullName);
    AuthenticationChallenge createStudentChallenge(String userId);
}