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
public class UpdateEventRequest {

    @Size(min = 3, max = 200)
    private String eventName;

    @Size(max = 100)
    private String gameName;

    @Size(max = 5000)
    private String description;

    private LocalDate eventDate;

    private LocalTime eventStartTime;

    private LocalTime eventEndTime;

    private LocalDateTime registrationOpenDate;

    private LocalDateTime registrationCloseDate;

    private ParticipationType participationType;

    @Min(1)
    @Max(100)
    private Integer teamSize;

    @Min(2)
    @Max(1000)
    private Integer maxParticipants;

    @Min(2)
    private Integer minParticipants;

    @Size(max = 500)
    private String allowedRanks;

    @Size(max = 50)
    private String platform;

    private Boolean isPaid;

    @Min(0)
    private Double entryFee;

    @Min(0)
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

    private EventVisibility visibility;
}

