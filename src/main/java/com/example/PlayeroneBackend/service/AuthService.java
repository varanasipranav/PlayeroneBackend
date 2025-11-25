package com.example.PlayeroneBackend.service;

import com.example.PlayeroneBackend.dto.AuthResponse;
import com.example.PlayeroneBackend.dto.LoginRequest;
import com.example.PlayeroneBackend.dto.SignupRequest;
import com.example.PlayeroneBackend.entity.User;
import com.example.PlayeroneBackend.enums.UserRole;
import com.example.PlayeroneBackend.repository.UserRepository;
import com.example.PlayeroneBackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signup(SignupRequest request) {
        // Validate username, email, and phone number uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number is already in use!");
        }

        // Validate Player-specific fields
        if (request.getRole() == UserRole.PLAYER) {
            if (request.getBgmiGameId() == null || request.getBgmiGameId().isEmpty()) {
                throw new RuntimeException("BGMI Game ID is required for Player role!");
            }
            if (request.getBgmiInGameName() == null || request.getBgmiInGameName().isEmpty()) {
                throw new RuntimeException("BGMI In-Game Name is required for Player role!");
            }
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        // Set Player-specific fields if role is PLAYER
        if (request.getRole() == UserRole.PLAYER) {
            user.setBgmiGameId(request.getBgmiGameId());
            user.setBgmiInGameName(request.getBgmiInGameName());
        }

        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername());

        // Build response
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .bgmiGameId(user.getBgmiGameId())
                .bgmiInGameName(user.getBgmiInGameName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Generate JWT token
        String token = jwtUtil.generateToken(request.getUsername());

        // Get user details
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build response
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .bgmiGameId(user.getBgmiGameId())
                .bgmiInGameName(user.getBgmiInGameName())
                .build();
    }
}

