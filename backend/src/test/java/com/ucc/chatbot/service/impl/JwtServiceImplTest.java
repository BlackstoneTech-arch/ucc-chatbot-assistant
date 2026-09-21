package com.ucc.chatbot.service.impl;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-that-is-at-least-32-bytes";

    @Test
    void generatedTokenContainsEmailAndRole() throws Exception {
        JwtServiceImpl jwtService = configuredService(60_000);

        String token = jwtService.generateToken("student@example.com", "STUDENT");
        Claims claims = jwtService.extractClaims(token);

        assertEquals("student@example.com", claims.getSubject());
        assertEquals("STUDENT", claims.get("role", String.class));
        assertTrue(jwtService.isTokenValid(token, "student@example.com"));
    }

    @Test
    void tokenWithDifferentEmailIsInvalid() throws Exception {
        JwtServiceImpl jwtService = configuredService(60_000);
        String token = jwtService.generateToken("student@example.com", "STUDENT");

        assertFalse(jwtService.isTokenValid(token, "other@example.com"));
    }

    @Test
    void expiredTokenIsInvalid() throws Exception {
        JwtServiceImpl jwtService = configuredService(-1);
        String token = jwtService.generateToken("student@example.com", "STUDENT");

        assertFalse(jwtService.isTokenValid(token, "student@example.com"));
    }

    @Test
    void malformedTokenIsInvalid() throws Exception {
        JwtServiceImpl jwtService = configuredService(60_000);

        assertFalse(jwtService.isTokenValid("not-a-jwt", "student@example.com"));
    }

    private static JwtServiceImpl configuredService(long expirationMs) throws Exception {
        JwtServiceImpl service = new JwtServiceImpl();
        setField(service, "secret", SECRET);
        setField(service, "jwtExpirationMs", expirationMs);
        return service;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}