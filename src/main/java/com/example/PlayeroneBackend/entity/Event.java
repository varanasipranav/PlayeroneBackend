package com.example.PlayeroneBackend.entity;

import com.example.PlayeroneBackend.enums.EventStatus;
import com.example.PlayeroneBackend.enums.EventVisibility;
import com.example.PlayeroneBackend.enums.ParticipationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String eventId; // Custom event ID like "EVT-2024-001"

    @Column(nullable = false, length = 200)
    private String eventName;

    @Column(nullable = false, length = 100)
    private String gameName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Event Timing
    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private LocalTime eventStartTime;

    @Column(nullable = false)
    private LocalTime eventEndTime;

    @Column(nullable = false)
    private LocalDateTime registrationOpenDate;

    @Column(nullable = false)
    private LocalDateTime registrationCloseDate;

    // Participation Details
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationType participationType;

    @Column(nullable = false)
    private Integer teamSize;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer minParticipants;

    @Column(length = 500)
    private String allowedRanks; // Comma-separated ranks

    @Column(length = 50)
    private String platform; // PC, Mobile, Console

    // Payment & Prizes
    @Column(nullable = false)
    private Boolean isPaid = false;

    @Column
    private Double entryFee = 0.0;

    @Column
    private Double prizePool = 0.0;

    @Column(columnDefinition = "JSON")
    private String prizeDistribution; // JSON string: {"1st": 5000, "2nd": 3000, "3rd": 2000}

    @Column(columnDefinition = "TEXT")
    private String refundPolicy;

    // Game Configuration
    @Column(length = 100)
    private String map;

    @Column(length = 100)
    private String mode;

    @Column(length = 50)
    private String serverRegion;

    @Column(length = 100)
    private String roomId;

    @Column(length = 100)
    private String roomPassword;

    @Column(columnDefinition = "TEXT")
    private String rules;

    // Organizer Details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    @JsonIgnore
    private User organizer;

    @Column(nullable = false, length = 200)
    private String organizerName;

    @Column(length = 200)
    private String contact;

    // Registrations
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<EventRegistration> registrations = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer slotsFilled = 0;

    // Event Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    // Media & Results
    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 500)
    private String liveStreamLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(length = 500)
    private String highlightVideoUrl;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventVisibility visibility = EventVisibility.PUBLIC;

    // Audit Fields
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isFull() {
        return slotsFilled >= maxParticipants;
    }

    public boolean isRegistrationOpen() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(registrationOpenDate) && now.isBefore(registrationCloseDate)
                && status == EventStatus.REGISTRATION_OPEN && !isFull();
    }

    public void incrementSlotsFilled() {
        this.slotsFilled++;
    }

    public void decrementSlotsFilled() {
        if (this.slotsFilled > 0) {
            this.slotsFilled--;
        }
    }
}

