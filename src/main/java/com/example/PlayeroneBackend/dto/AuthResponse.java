package com.example.PlayeroneBackend.dto;

import com.example.PlayeroneBackend.enums.UserRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private UserRole role;

    // Player-specific fields
    private String bgmiGameId;
    private String bgmiInGameName;
}

