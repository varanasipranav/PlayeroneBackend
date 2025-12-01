package com.example.PlayeroneBackend.controller;

import com.example.PlayeroneBackend.dto.RegistrationResponse;
import com.example.PlayeroneBackend.service.EventRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/registrations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(
        name = "Event Registration Management",
        description = "APIs for organizers and admins to manage event registrations. " +
                "View all registrations, confirm or reject registrations, verify payments, and export participant lists."
)
@SecurityRequirement(name = "bearerAuth")
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(
            summary = "Get all event registrations",
            description = "Retrieves paginated list of all registrations for a specific event. " +
                    "Shows registration status (PENDING, CONFIRMED, REJECTED, CANCELLED), payment details, " +
                    "team information, and player BGMI IDs. Only accessible by event organizer or admins."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registrations retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized - must be event organizer or admin"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            )
    })
    public ResponseEntity<Page<RegistrationResponse>> getEventRegistrations(
            @Parameter(description = "Event ID to get registrations for", required = true)
            @PathVariable Long eventId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Items per page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field", example = "registeredAt")
            @RequestParam(defaultValue = "registeredAt") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<RegistrationResponse> registrations = registrationService.getEventRegistrations(eventId, pageable);
        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/event/{eventId}/confirmed")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(
            summary = "Get confirmed registrations",
            description = "Get list of all confirmed/approved registrations for an event. " +
                    "Useful for generating participant lists, sharing room details, or exporting data for tournament management. " +
                    "Only shows registrations with CONFIRMED status."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Confirmed registrations retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to view registrations"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Event not found"
            )
    })
    public ResponseEntity<List<RegistrationResponse>> getConfirmedRegistrations(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long eventId) {
        List<RegistrationResponse> registrations = registrationService.getConfirmedRegistrations(eventId);
        return ResponseEntity.ok(registrations);
    }

    @PutMapping("/{registrationId}/confirm")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(
            summary = "Confirm a registration",
            description = "Approve/confirm a pending registration. For paid events, verify payment before confirming. " +
                    "Once confirmed, the player is officially registered for the tournament and will receive room details."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration confirmed successfully",
                    content = @Content(schema = @Schema(implementation = RegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Registration not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to confirm this registration"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Registration is already confirmed or event is full"
            )
    })
    public ResponseEntity<RegistrationResponse> confirmRegistration(
            @Parameter(description = "Registration ID to confirm", required = true)
            @PathVariable Long registrationId) {
        RegistrationResponse response = registrationService.confirmRegistration(registrationId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{registrationId}/reject")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(
            summary = "Reject a registration",
            description = "Reject/decline a registration with an optional reason. " +
                    "Common reasons include: payment not verified, invalid BGMI ID, duplicate registration, " +
                    "player rank not allowed, or event is full. Player will be notified of rejection."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration rejected successfully",
                    content = @Content(schema = @Schema(implementation = RegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Registration not found"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not authorized to reject this registration"
            )
    })
    public ResponseEntity<RegistrationResponse> rejectRegistration(
            @Parameter(description = "Registration ID to reject", required = true)
            @PathVariable Long registrationId,
            @Parameter(description = "Reason for rejection (optional)", example = "Payment not verified")
            @RequestParam(required = false) String reason) {
        RegistrationResponse response = registrationService.rejectRegistration(registrationId, reason);
        return ResponseEntity.ok(response);
    }
}

