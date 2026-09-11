package com.micarro.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.micarro.backend.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {

        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {

        Date issuedAt = new Date();
        Date expiration = new Date(
                issuedAt.getTime() + expirationSeconds * 1000
        );

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("name", user.getName())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, String email) {

        try {
            Claims claims = parseClaims(token);

            return email.equals(claims.getSubject())
                    && claims.getExpiration().after(new Date());

        } catch (Exception exception) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims parseClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
