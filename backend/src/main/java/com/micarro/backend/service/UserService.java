package com.micarro.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.UserProfileResponse;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfileByEmail(String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
