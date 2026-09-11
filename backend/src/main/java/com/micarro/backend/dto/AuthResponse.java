package com.micarro.backend.dto;

public class AuthResponse {

    private final String token;
    private final String tokenType;
    private final long expiresIn;
    private final Long userId;
    private final String name;
    private final String email;

    public AuthResponse(
            String token,
            String tokenType,
            long expiresIn,
            Long userId,
            String name,
            String email) {

        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
