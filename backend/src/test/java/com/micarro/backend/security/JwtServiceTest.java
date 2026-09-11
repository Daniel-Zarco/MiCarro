package com.micarro.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.micarro.backend.entity.User;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-for-micarro-tests-32bytes!!";

    private final JwtService jwtService = new JwtService(SECRET, 3600);

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setName("Dani");
        user.setEmail("dani@example.com");
        return user;
    }

    @Test
    void generateToken_containsUserEmail() {

        String token = jwtService.generateToken(user());

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo("dani@example.com");
    }

    @Test
    void isValid_trueForMatchingEmail() {

        String token = jwtService.generateToken(user());

        assertThat(jwtService.isValid(token, "dani@example.com")).isTrue();
    }

    @Test
    void isValid_falseForDifferentEmail() {

        String token = jwtService.generateToken(user());

        assertThat(jwtService.isValid(token, "other@example.com")).isFalse();
    }

    @Test
    void isValid_falseForMalformedToken() {

        assertThat(jwtService.isValid("not-a-valid-token", "dani@example.com"))
                .isFalse();
    }
}
