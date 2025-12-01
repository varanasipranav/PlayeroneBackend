package com.example.PlayeroneBackend.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterEventRequest {

    @Size(max = 100, message = "Team name must not exceed 100 characters")
    private String teamName;

    @Size(max = 1000, message = "Additional notes must not exceed 1000 characters")
    private String additionalNotes;

    private String transactionId; // For paid events
}

