package com.micarro.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.AuthResponse;
import com.micarro.backend.dto.LoginRequest;
import com.micarro.backend.dto.RegisterRequest;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.UserRepository;
import com.micarro.backend.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El email ya está registrado"
            );
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userRepository.save(user);

        return buildResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciales inválidas"
                ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales inválidas"
            );
        }

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
