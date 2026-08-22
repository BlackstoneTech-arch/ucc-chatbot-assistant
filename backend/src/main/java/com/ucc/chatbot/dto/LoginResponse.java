package com.ucc.chatbot.dto;

public class LoginResponse {
    private String token;
    private String userId;
    private String email;
    private String fullName;
    private String role;

    public LoginResponse() {}

    public LoginResponse(String token, String userId, String email, String fullName, String role) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public static LoginResponse builder() {
        return new LoginResponse();
    }

    public LoginResponse token(String token) { this.token = token; return this; }
    public LoginResponse userId(String userId) { this.userId = userId; return this; }
    public LoginResponse email(String email) { this.email = email; return this; }
    public LoginResponse fullName(String fullName) { this.fullName = fullName; return this; }
    public LoginResponse role(String role) { this.role = role; return this; }
    public LoginResponse build() { return this; }

    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
}
