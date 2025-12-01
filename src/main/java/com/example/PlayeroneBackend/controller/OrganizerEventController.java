package com.example.PlayeroneBackend.controller;

import com.example.PlayeroneBackend.dto.CreateEventRequest;
import com.example.PlayeroneBackend.dto.EventResponse;
import com.example.PlayeroneBackend.dto.MessageResponse;
import com.example.PlayeroneBackend.dto.UpdateEventRequest;
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
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "bearerAuth")
@Tag(
        name = "Organizer Event Management",
        description = "APIs for organizers and admins to create, update, and manage gaming events. " +
                "Organizers can create tournaments, set prize pools, manage registrations, and track event status."
)
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@RequestMapping("/api/events/organizer")
@RestController
public class OrganizerEventController {

    private final EventService eventService;

    @Operation(
            summary = "Create a new event",
            description = "Creates a new gaming event with all details including timing, participation type (SOLO/DUO/SQUAD), " +
                    "prize pool, entry fee, and game configuration. Event is created in DRAFT status by default."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Event created successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User is not an organizer or admin"
            )
    })
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        try {
            EventResponse response = eventService.createEvent(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Update an existing event",
            description = "Updates event details. Only the organizer who created the event or admins can update it. " +
                    "Cannot update events that have already started or are completed."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event updated successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to update this event"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot update event in current status"
            )
    })
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @Parameter(description = "ID of the event to update", required = true)
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        try {
            EventResponse response = eventService.updateEvent(eventId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Delete an event",
            description = "Permanently deletes an event. Only events with no registrations can be deleted. " +
                    "Only the organizer who created the event or admins can delete it."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot delete event with existing registrations"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to delete this event"
            )
    })
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<MessageResponse> deleteEvent(
            @Parameter(description = "ID of the event to delete", required = true)
            @PathVariable Long eventId) {
        try {
            eventService.deleteEvent(eventId);
            return ResponseEntity.ok(new MessageResponse("Event deleted successfully"));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(ise.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(
            summary = "Publish an event",
            description = "Changes event status from DRAFT to REGISTRATION_OPEN, making it visible to players. " +
                    "Players can start registering once an event is published."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event published successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Event is not in DRAFT status"
            )
    })
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PostMapping("/{eventId}/publish")
    public ResponseEntity<EventResponse> publishEvent(
            @Parameter(description = "ID of the event to publish", required = true)
            @PathVariable Long eventId) {
        try {
            EventResponse response = eventService.publishEvent(eventId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Cancel an event",
            description = "Cancels an event with an optional reason. Registered players will be notified. " +
                    "Refunds should be processed according to the refund policy."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Event cancelled successfully",
                    content = @Content(schema = @Schema(implementation = EventResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Event cannot be cancelled in current status"
            )
    })
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PostMapping("/{eventId}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(
            @Parameter(description = "ID of the event to cancel", required = true)
            @PathVariable Long eventId,
            @Parameter(description = "Reason for cancellation (optional)")
            @RequestParam(required = false) String reason) {
        try {
            EventResponse response = eventService.cancelEvent(eventId, reason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Get my created events",
            description = "Retrieves paginated list of all events created by the logged-in organizer. " +
                    "Supports sorting by various fields and filtering by status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Events retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @GetMapping("/my-events")
    public ResponseEntity<Page<EventResponse>> getMyEvents(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<EventResponse> events = eventService.getMyEvents(pageable);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Get event details by ID",
            description = "Retrieves detailed information about a specific event including registration count, " +
                    "prize details, and game configuration."
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
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(
            @Parameter(description = "ID of the event", required = true)
            @PathVariable Long eventId) {
        try {
            EventResponse response = eventService.getEventById(eventId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
