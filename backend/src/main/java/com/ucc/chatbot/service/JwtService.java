package com.ucc.chatbot.service;

import io.jsonwebtoken.Claims;

public interface JwtService {
    String generateToken(String email, String role);
    String extractEmail(String token);
    Claims extractClaims(String token);
    boolean isTokenValid(String token, String email);
}
