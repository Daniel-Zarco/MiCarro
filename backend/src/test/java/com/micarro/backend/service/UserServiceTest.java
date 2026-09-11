package com.micarro.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.UserProfileResponse;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getProfileByEmail_returnsIdNameAndEmail() {

        User user = new User();
        user.setId(1L);
        user.setName("Dani");
        user.setEmail("dani@example.com");
        user.setPassword("hashed-password");

        when(userRepository.findByEmailIgnoreCase("dani@example.com"))
                .thenReturn(Optional.of(user));

        UserProfileResponse profile =
                userService.getProfileByEmail("dani@example.com");

        assertThat(profile.getId()).isEqualTo(1L);
        assertThat(profile.getName()).isEqualTo("Dani");
        assertThat(profile.getEmail()).isEqualTo("dani@example.com");
    }

    @Test
    void getProfileByEmail_throwsNotFoundWhenUserDoesNotExist() {

        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> userService.getProfileByEmail("nobody@example.com"))
                .satisfies(exception -> assertThat(exception.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
