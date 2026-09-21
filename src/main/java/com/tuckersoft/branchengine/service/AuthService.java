package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.AuthResponse;
import com.tuckersoft.branchengine.dto.LoginRequest;
import com.tuckersoft.branchengine.dto.RegisterRequest;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import com.tuckersoft.branchengine.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("Ya existe una cuenta con ese email.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        // Quien se registra siempre queda como ROLE_USER, sin excepcion.
        user.setRole("ROLE_USER");
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.unauthorized("Email o contrasena incorrectos."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("Email o contrasena incorrectos.");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
