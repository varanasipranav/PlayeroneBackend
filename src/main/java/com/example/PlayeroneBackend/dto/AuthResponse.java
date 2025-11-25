package com.example.PlayeroneBackend.dto;

import com.example.PlayeroneBackend.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

