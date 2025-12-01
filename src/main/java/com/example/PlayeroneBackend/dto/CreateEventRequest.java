package com.example.PlayeroneBackend.dto;

import com.example.PlayeroneBackend.enums.EventVisibility;
import com.example.PlayeroneBackend.enums.ParticipationType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(min = 3, max = 200, message = "Event name must be between 3 and 200 characters")
    private String eventName;

    @NotBlank(message = "Game name is required")
    @Size(max = 100)
    private String gameName;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDate eventDate;

    @NotNull(message = "Event start time is required")
    private LocalTime eventStartTime;

    @NotNull(message = "Event end time is required")
    private LocalTime eventEndTime;

    @NotNull(message = "Registration open date is required")
    private LocalDateTime registrationOpenDate;

    @NotNull(message = "Registration close date is required")
    private LocalDateTime registrationCloseDate;

    @NotNull(message = "Participation type is required")
    private ParticipationType participationType;

    @NotNull(message = "Team size is required")
    @Min(value = 1, message = "Team size must be at least 1")
    @Max(value = 100, message = "Team size must not exceed 100")
    private Integer teamSize;

    @NotNull(message = "Max participants is required")
    @Min(value = 2, message = "Max participants must be at least 2")
    @Max(value = 1000, message = "Max participants must not exceed 1000")
    private Integer maxParticipants;

    @NotNull(message = "Min participants is required")
    @Min(value = 2, message = "Min participants must be at least 2")
    private Integer minParticipants;

    @Size(max = 500)
    private String allowedRanks;

    @Size(max = 50)
    private String platform;

    @NotNull(message = "isPaid field is required")
    private Boolean isPaid;

    @Min(value = 0, message = "Entry fee cannot be negative")
    private Double entryFee;

    @Min(value = 0, message = "Prize pool cannot be negative")
    private Double prizePool;

    private String prizeDistribution;

    @Size(max = 2000)
    private String refundPolicy;

    @Size(max = 100)
    private String map;

    @Size(max = 100)
    private String mode;

    @Size(max = 50)
    private String serverRegion;

    @Size(max = 100)
    private String roomId;

    @Size(max = 100)
    private String roomPassword;

    @Size(max = 5000)
    private String rules;

    @Size(max = 200)
    private String contact;

    @Size(max = 500)
    private String thumbnailUrl;

    @Size(max = 500)
    private String liveStreamLink;

    @Size(max = 2000)
    private String remarks;

    @NotNull(message = "Visibility is required")
    private EventVisibility visibility;
}

