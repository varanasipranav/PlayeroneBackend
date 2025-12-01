package com.example.PlayeroneBackend.controller;

import com.example.PlayeroneBackend.dto.EventResponse;
import com.example.PlayeroneBackend.dto.MessageResponse;
import com.example.PlayeroneBackend.dto.RegisterEventRequest;
import com.example.PlayeroneBackend.dto.RegistrationResponse;
import com.example.PlayeroneBackend.service.EventRegistrationService;
import com.example.PlayeroneBackend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/player")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(
        name = "Player Event Management",
        description = "APIs for players to browse events, search tournaments, register for events, and manage their registrations. " +
                "Includes public endpoints for browsing and authenticated endpoints for registration."
)
@SecurityRequirement(name = "bearerAuth")
public class PlayerEventController {

    private final EventService eventService;
    private final EventRegistrationService registrationService;

    @GetMapping("/public")
    @Operation(
            summary = "Browse public events",
            description = "Get paginated list of all publicly visible events with registration open. " +
                    "No authentication required. Useful for players to browse available tournaments."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Events retrieved successfully"
            )
    })
    public ResponseEntity<Page<EventResponse>> getPublicEvents(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field (eventDate, prizePool, entryFee)", example = "eventDate")
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)", example = "ASC")
            @RequestParam(defaultValue = "ASC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EventResponse> events = eventService.getPublicEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "Get upcoming events",
            description = "Get all events scheduled in the future with open registration, sorted by event date. " +
                    "Shows events in chronological order starting from the nearest date."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Upcoming events retrieved successfully"
            )
    })
    public ResponseEntity<Page<EventResponse>> getUpcomingEvents(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());
        Page<EventResponse> events = eventService.getUpcomingEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search events",
            description = "Search events by keyword in event name, game name, or description. " +
                    "Case-insensitive search across multiple fields."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully"
            )
    })
    public ResponseEntity<Page<EventResponse>> searchEvents(
            @Parameter(description = "Search keyword", required = true, example = "BGMI")
            @RequestParam String keyword,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> events = eventService.searchEvents(keyword, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/game/{gameName}")
    @Operation(
            summary = "Get events by game",
            description = "Get all events for a specific game (e.g., BGMI, Free Fire, Call of Duty). " +
                    "Useful for players who want to see tournaments for their favorite games."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Events for the game retrieved successfully"
            )
    })
    public ResponseEntity<Page<EventResponse>> getEventsByGame(
            @Parameter(description = "Game name", required = true, example = "BGMI")
            @PathVariable String gameName,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> events = eventService.getEventsByGame(gameName, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    @Operation(
            summary = "Get event details",
            description = "Get complete details of a specific event including prize pool, rules, room details, " +
                    "registration status, and current participant count."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            )
    })
    public ResponseEntity<EventResponse> getEventById(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long eventId) {
        EventResponse response = eventService.getEventById(eventId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/register")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(
            summary = "Register for an event",
            description = "Register for a gaming tournament. Players must provide team name for DUO/SQUAD events. " +
                    "For paid events, transaction ID is required. Registration is subject to available slots and open registration period."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Successfully registered for the event",
                    content = @Content(schema = @Schema(implementation = RegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data or event is full"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Already registered for this event"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Registration is closed or user is not a player"
            )
    })
    public ResponseEntity<RegistrationResponse> registerForEvent(
            @Parameter(description = "Event ID to register for", required = true)
            @PathVariable Long eventId,
            @Valid @RequestBody RegisterEventRequest request) {
        RegistrationResponse response = registrationService.registerForEvent(eventId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/registration/{registrationId}")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(
            summary = "Cancel event registration",
            description = "Cancel your registration for an event. Check refund policy for paid events. " +
                    "Registration can only be cancelled before the event starts."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration cancelled successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Registration not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot cancel - event has already started"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to cancel this registration"
            )
    })
    public ResponseEntity<MessageResponse> cancelRegistration(
            @Parameter(description = "Registration ID to cancel", required = true)
            @PathVariable Long registrationId) {
        registrationService.cancelRegistration(registrationId);
        return ResponseEntity.ok(new MessageResponse("Registration cancelled successfully"));
    }

    @GetMapping("/my-registrations")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(
            summary = "Get my event registrations",
            description = "Get paginated list of all your event registrations including status (PENDING, CONFIRMED, CANCELLED). " +
                    "Shows upcoming events, payment status, and team details."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registrations retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            )
    })
    public ResponseEntity<Page<RegistrationResponse>> getMyRegistrations(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "registeredAt")
            @RequestParam(defaultValue = "registeredAt") String sortBy,
            @Parameter(description = "Sort direction", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<RegistrationResponse> registrations = registrationService.getMyRegistrations(pageable);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/registration/{registrationId}")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(
            summary = "Get registration details",
            description = "Get detailed information about a specific registration including payment status, " +
                    "team details, and confirmation status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Registration not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to view this registration"
            )
    })
    public ResponseEntity<RegistrationResponse> getRegistrationById(
            @Parameter(description = "Registration ID", required = true)
            @PathVariable Long registrationId) {
        RegistrationResponse response = registrationService.getRegistrationById(registrationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}/is-registered")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(
            summary = "Check registration status",
            description = "Check if the current player is already registered for a specific event. " +
                    "Returns true if registered, false otherwise. Useful before showing register button."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration status retrieved"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            )
    })
    public ResponseEntity<Boolean> isUserRegistered(
            @Parameter(description = "Event ID to check", required = true)
            @PathVariable Long eventId) {
        boolean isRegistered = registrationService.isUserRegistered(eventId);
        return ResponseEntity.ok(isRegistered);
    }
}

