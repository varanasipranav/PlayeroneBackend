package com.example.PlayeroneBackend.service;

import com.example.PlayeroneBackend.dto.RegisterEventRequest;
import com.example.PlayeroneBackend.dto.RegistrationResponse;
import com.example.PlayeroneBackend.entity.Event;
import com.example.PlayeroneBackend.entity.EventRegistration;
import com.example.PlayeroneBackend.entity.User;
import com.example.PlayeroneBackend.enums.RegistrationStatus;
import com.example.PlayeroneBackend.enums.UserRole;
import com.example.PlayeroneBackend.exception.*;
import com.example.PlayeroneBackend.repository.EventRegistrationRepository;
import com.example.PlayeroneBackend.repository.EventRepository;
import com.example.PlayeroneBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public RegistrationResponse registerForEvent(Long eventId, RegisterEventRequest request) {
        User user = getCurrentUser();

        // Validate user role
        if (user.getRole() != UserRole.PLAYER) {
            throw new UnauthorizedException("Only players can register for events");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        // Check if registration is open
        if (!event.isRegistrationOpen()) {
            throw new RegistrationClosedException("Registration is not open for this event");
        }

        // Check if event is full
        if (event.isFull()) {
            throw new EventFullException("Event is full. No more slots available");
        }

        // Check if user already registered
        if (registrationRepository.existsByEventAndUser(event, user)) {
            throw new DuplicateRegistrationException("You have already registered for this event");
        }

        // Validate payment for paid events
        if (event.getIsPaid()) {
            if (request.getTransactionId() == null || request.getTransactionId().isEmpty()) {
                throw new BadRequestException("Transaction ID is required for paid events");
            }
        }

        // Create registration
        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(event.getIsPaid() ? RegistrationStatus.PENDING : RegistrationStatus.CONFIRMED)
                .teamName(request.getTeamName())
                .additionalNotes(request.getAdditionalNotes())
                .transactionId(request.getTransactionId())
                .amountPaid(event.getEntryFee())
                .paymentVerified(false)
                .build();

        EventRegistration savedRegistration = registrationRepository.save(registration);

        // Increment slots filled
        event.incrementSlotsFilled();
        eventRepository.save(event);

        log.info("User {} registered for event {}", user.getUsername(), event.getEventId());

        return mapToRegistrationResponse(savedRegistration);
    }

    @Transactional
    public void cancelRegistration(Long registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        User currentUser = getCurrentUser();

        // Check authorization
        if (!registration.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to cancel this registration");
        }

        // Check if already cancelled
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Registration is already cancelled");
        }

        // Update registration status
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registration.setCancellationReason("Cancelled by user");
        registrationRepository.save(registration);

        // Decrement slots filled
        Event event = registration.getEvent();
        event.decrementSlotsFilled();
        eventRepository.save(event);

        log.info("Registration cancelled: {} for event: {}", registrationId, event.getEventId());
    }

    @Transactional
    public RegistrationResponse confirmRegistration(Long registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        User currentUser = getCurrentUser();
        Event event = registration.getEvent();

        // Check authorization - only organizer or admin can confirm
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to confirm this registration");
        }

        if (registration.getStatus() == RegistrationStatus.CONFIRMED) {
            throw new BadRequestException("Registration is already confirmed");
        }

        registration.setStatus(RegistrationStatus.CONFIRMED);
        if (event.getIsPaid()) {
            registration.setPaymentVerified(true);
        }

        EventRegistration updatedRegistration = registrationRepository.save(registration);
        log.info("Registration confirmed: {} for event: {}", registrationId, event.getEventId());

        return mapToRegistrationResponse(updatedRegistration);
    }

    @Transactional
    public RegistrationResponse rejectRegistration(Long registrationId, String reason) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        User currentUser = getCurrentUser();
        Event event = registration.getEvent();

        // Check authorization - only organizer or admin can reject
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to reject this registration");
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setCancellationReason(reason);

        // Decrement slots filled
        event.decrementSlotsFilled();
        eventRepository.save(event);

        EventRegistration updatedRegistration = registrationRepository.save(registration);
        log.info("Registration rejected: {} for event: {} - Reason: {}", registrationId, event.getEventId(), reason);

        return mapToRegistrationResponse(updatedRegistration);
    }

    @Transactional(readOnly = true)
    public Page<RegistrationResponse> getMyRegistrations(Pageable pageable) {
        User currentUser = getCurrentUser();
        return registrationRepository.findByUser(currentUser, pageable)
                .map(this::mapToRegistrationResponse);
    }

    @Transactional(readOnly = true)
    public Page<RegistrationResponse> getEventRegistrations(Long eventId, Pageable pageable) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User currentUser = getCurrentUser();

        // Check authorization - only organizer or admin can view all registrations
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to view registrations for this event");
        }

        return registrationRepository.findByEvent(event, pageable)
                .map(this::mapToRegistrationResponse);
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getConfirmedRegistrations(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User currentUser = getCurrentUser();

        // Check authorization
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to view registrations for this event");
        }

        return registrationRepository.findConfirmedRegistrationsByEventId(eventId).stream()
                .map(this::mapToRegistrationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RegistrationResponse getRegistrationById(Long registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with id: " + registrationId));

        User currentUser = getCurrentUser();

        // Check authorization - user can view their own registration, organizer can view all
        if (!registration.getUser().getId().equals(currentUser.getId()) &&
            !registration.getEvent().getOrganizer().getId().equals(currentUser.getId()) &&
            currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to view this registration");
        }

        return mapToRegistrationResponse(registration);
    }

    @Transactional(readOnly = true)
    public boolean isUserRegistered(Long eventId) {
        User currentUser = getCurrentUser();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        return registrationRepository.existsByEventAndUser(event, currentUser);
    }

    // Helper methods
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private RegistrationResponse mapToRegistrationResponse(EventRegistration registration) {
        return RegistrationResponse.builder()
                .id(registration.getId())
                .eventId(registration.getEvent().getId())
                .eventName(registration.getEvent().getEventName())
                .userId(registration.getUser().getId())
                .username(registration.getUser().getUsername())
                .userEmail(registration.getUser().getEmail())
                .status(registration.getStatus())
                .teamName(registration.getTeamName())
                .additionalNotes(registration.getAdditionalNotes())
                .transactionId(registration.getTransactionId())
                .amountPaid(registration.getAmountPaid())
                .paymentVerified(registration.getPaymentVerified())
                .registeredAt(registration.getRegisteredAt())
                .updatedAt(registration.getUpdatedAt())
                .build();
    }
}

