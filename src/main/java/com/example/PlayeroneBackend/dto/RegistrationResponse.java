package com.example.PlayeroneBackend.dto;

import com.example.PlayeroneBackend.enums.RegistrationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationResponse {
    private Long id;
    private Long eventId;
    private String eventName;
    private Long userId;
    private String username;
    private String userEmail;
    private RegistrationStatus status;
    private String teamName;
    private String additionalNotes;
    private String transactionId;
    private Double amountPaid;
    private Boolean paymentVerified;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
}

