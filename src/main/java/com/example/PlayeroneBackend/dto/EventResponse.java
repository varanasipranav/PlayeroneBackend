package com.example.PlayeroneBackend.dto;

import com.example.PlayeroneBackend.enums.EventStatus;
import com.example.PlayeroneBackend.enums.EventVisibility;
import com.example.PlayeroneBackend.enums.ParticipationType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String eventId;
    private String eventName;
    private String gameName;
    private String description;
    private LocalDate eventDate;
    private LocalTime eventStartTime;
    private LocalTime eventEndTime;
    private LocalDateTime registrationOpenDate;
    private LocalDateTime registrationCloseDate;
    private ParticipationType participationType;
    private Integer teamSize;
    private Integer maxParticipants;
    private Integer minParticipants;
    private String allowedRanks;
    private String platform;
    private Boolean isPaid;
    private Double entryFee;
    private Double prizePool;
    private String prizeDistribution;
    private String refundPolicy;
    private String map;
    private String mode;
    private String serverRegion;
    private String roomId;
    private String roomPassword;
    private String rules;
    private Long organizerId;
    private String organizerName;
    private String contact;
    private Integer slotsFilled;
    private Integer slotsAvailable;
    private EventStatus status;
    private String thumbnailUrl;
    private String liveStreamLink;
    private Long winnerId;
    private String highlightVideoUrl;
    private String remarks;
    private EventVisibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isRegistrationOpen;
}

