package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.AuthResponse;
import com.micarro.backend.dto.LoginRequest;
import com.micarro.backend.dto.RegisterRequest;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.UserRepository;
import com.micarro.backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET =
            "test-secret-key-for-micarro-tests-32bytes!!";

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                new JwtService(SECRET, 3600)
        );
    }

    private RegisterRequest registerRequest(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setName("Dani");
        request.setEmail(email);
        request.setPassword("password123");
        return request;
    }

    @Test
    void register_createsUserAndReturnsTokenWithoutPassword() {

        when(userRepository.existsByEmailIgnoreCase("dani@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        AuthResponse response =
                authService.register(registerRequest("Dani@Example.com"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("dani@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(saved.getPassword()).startsWith("$2");
    }

    @Test
    void register_throwsConflictWhenEmailAlreadyExists() {

        when(userRepository.existsByEmailIgnoreCase("dani@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> authService.register(registerRequest("dani@example.com"))
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_returnsTokenForValidCredentials() {

        User user = new User();
        user.setId(1L);
        user.setName("Dani");
        user.setEmail("dani@example.com");
        user.setPassword(passwordEncoder.encode("password123"));

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("dani@example.com");
        request.setPassword("password123");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_throwsUnauthorizedForWrongPassword() {

        User user = new User();
        user.setId(1L);
        user.setEmail("dani@example.com");
        user.setPassword(passwordEncoder.encode("password123"));

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("dani@example.com");
        request.setPassword("wrong-password");

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_throwsUnauthorizedForUnknownEmail() {

        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@example.com");
        request.setPassword("password123");

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
