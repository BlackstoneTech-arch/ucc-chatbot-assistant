package com.ucc.chatbot.config;

/**
 * CORS is centralized in {@link SecurityConfig} so the application has one
 * authoritative origin allow-list. Keeping a second MVC CORS configuration
 * here previously allowed the two configurations to drift apart.
 */
public final class CorsConfig {
    private CorsConfig() {
    }
}