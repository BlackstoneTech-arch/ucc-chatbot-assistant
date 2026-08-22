package com.ucc.chatbot.service;

import com.ucc.chatbot.dto.LoginRequest;
import com.ucc.chatbot.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
}
