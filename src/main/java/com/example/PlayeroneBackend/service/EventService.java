package com.example.PlayeroneBackend.service;

import com.example.PlayeroneBackend.dto.*;
import com.example.PlayeroneBackend.entity.Event;
import com.example.PlayeroneBackend.entity.User;
import com.example.PlayeroneBackend.enums.EventStatus;
import com.example.PlayeroneBackend.enums.EventVisibility;
import com.example.PlayeroneBackend.enums.UserRole;
import com.example.PlayeroneBackend.exception.BadRequestException;
import com.example.PlayeroneBackend.exception.ResourceNotFoundException;
import com.example.PlayeroneBackend.exception.UnauthorizedException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        User organizer = getCurrentUser();

        // Validate organizer role
        if (organizer.getRole() != UserRole.ORGANIZER && organizer.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only organizers and admins can create events");
        }

        // Validate dates
        validateEventDates(request.getEventDate(), request.getRegistrationOpenDate(),
                          request.getRegistrationCloseDate());

        // Validate participant limits
        if (request.getMinParticipants() > request.getMaxParticipants()) {
            throw new BadRequestException("Minimum participants cannot exceed maximum participants");
        }

        // Validate entry fee for paid events
        if (request.getIsPaid() && (request.getEntryFee() == null || request.getEntryFee() <= 0)) {
            throw new BadRequestException("Entry fee is required for paid events");
        }

        // Generate unique event ID
        String eventId = generateEventId();

        Event event = Event.builder()
                .eventId(eventId)
                .eventName(request.getEventName())
                .gameName(request.getGameName())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .eventStartTime(request.getEventStartTime())
                .eventEndTime(request.getEventEndTime())
                .registrationOpenDate(request.getRegistrationOpenDate())
                .registrationCloseDate(request.getRegistrationCloseDate())
                .participationType(request.getParticipationType())
                .teamSize(request.getTeamSize())
                .maxParticipants(request.getMaxParticipants())
                .minParticipants(request.getMinParticipants())
                .allowedRanks(request.getAllowedRanks())
                .platform(request.getPlatform())
                .isPaid(request.getIsPaid())
                .entryFee(request.getEntryFee())
                .prizePool(request.getPrizePool())
                .prizeDistribution(request.getPrizeDistribution())
                .refundPolicy(request.getRefundPolicy())
                .map(request.getMap())
                .mode(request.getMode())
                .serverRegion(request.getServerRegion())
                .roomId(request.getRoomId())
                .roomPassword(request.getRoomPassword())
                .rules(request.getRules())
                .organizer(organizer)
                .organizerName(organizer.getUsername())
                .contact(request.getContact())
                .slotsFilled(0)
                .status(EventStatus.DRAFT)
                .thumbnailUrl(request.getThumbnailUrl())
                .liveStreamLink(request.getLiveStreamLink())
                .remarks(request.getRemarks())
                .visibility(request.getVisibility())
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event created: {} by organizer: {}", savedEvent.getEventId(), organizer.getUsername());

        return mapToEventResponse(savedEvent);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User currentUser = getCurrentUser();

        // Check authorization
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to update this event");
        }

        // Update only non-null fields
        if (request.getEventName() != null) event.setEventName(request.getEventName());
        if (request.getGameName() != null) event.setGameName(request.getGameName());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getEventStartTime() != null) event.setEventStartTime(request.getEventStartTime());
        if (request.getEventEndTime() != null) event.setEventEndTime(request.getEventEndTime());
        if (request.getRegistrationOpenDate() != null) event.setRegistrationOpenDate(request.getRegistrationOpenDate());
        if (request.getRegistrationCloseDate() != null) event.setRegistrationCloseDate(request.getRegistrationCloseDate());
        if (request.getParticipationType() != null) event.setParticipationType(request.getParticipationType());
        if (request.getTeamSize() != null) event.setTeamSize(request.getTeamSize());
        if (request.getMaxParticipants() != null) {
            if (request.getMaxParticipants() < event.getSlotsFilled()) {
                throw new BadRequestException("Cannot reduce max participants below current registrations");
            }
            event.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getMinParticipants() != null) event.setMinParticipants(request.getMinParticipants());
        if (request.getAllowedRanks() != null) event.setAllowedRanks(request.getAllowedRanks());
        if (request.getPlatform() != null) event.setPlatform(request.getPlatform());
        if (request.getIsPaid() != null) event.setIsPaid(request.getIsPaid());
        if (request.getEntryFee() != null) event.setEntryFee(request.getEntryFee());
        if (request.getPrizePool() != null) event.setPrizePool(request.getPrizePool());
        if (request.getPrizeDistribution() != null) event.setPrizeDistribution(request.getPrizeDistribution());
        if (request.getRefundPolicy() != null) event.setRefundPolicy(request.getRefundPolicy());
        if (request.getMap() != null) event.setMap(request.getMap());
        if (request.getMode() != null) event.setMode(request.getMode());
        if (request.getServerRegion() != null) event.setServerRegion(request.getServerRegion());
        if (request.getRoomId() != null) event.setRoomId(request.getRoomId());
        if (request.getRoomPassword() != null) event.setRoomPassword(request.getRoomPassword());
        if (request.getRules() != null) event.setRules(request.getRules());
        if (request.getContact() != null) event.setContact(request.getContact());
        if (request.getThumbnailUrl() != null) event.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getLiveStreamLink() != null) event.setLiveStreamLink(request.getLiveStreamLink());
        if (request.getRemarks() != null) event.setRemarks(request.getRemarks());
        if (request.getVisibility() != null) event.setVisibility(request.getVisibility());

        Event updatedEvent = eventRepository.save(event);
        log.info("Event updated: {}", updatedEvent.getEventId());

        return mapToEventResponse(updatedEvent);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User currentUser = getCurrentUser();

        // Check authorization
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to delete this event");
        }

        // Check if event has registrations
        if (event.getSlotsFilled() > 0) {
            throw new BadRequestException("Cannot delete event with existing registrations. Cancel the event instead.");
        }

        eventRepository.delete(event);
        log.info("Event deleted: {}", event.getEventId());
    }

    @Transactional
    public EventResponse publishEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User currentUser = getCurrentUser();

        // Check authorization
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to publish this event");
        }

        // Update status based on registration dates
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getRegistrationOpenDate())) {
            event.setStatus(EventStatus.UPCOMING);
        } else if (now.isBefore(event.getRegistrationCloseDate())) {
            event.setStatus(EventStatus.REGISTRATION_OPEN);
        } else {
            event.setStatus(EventStatus.REGISTRATION_CLOSED);
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Event published: {}", updatedEvent.getEventId());

        return mapToEventResponse(updatedEvent);
    }

    @Transactional
    public EventResponse cancelEvent(Long eventId, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        User currentUser = getCurrentUser();

        // Check authorization
        if (!event.getOrganizer().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("You are not authorized to cancel this event");
        }

        event.setStatus(EventStatus.CANCELLED);
        event.setRemarks(reason);

        Event updatedEvent = eventRepository.save(event);
        log.info("Event cancelled: {} - Reason: {}", updatedEvent.getEventId(), reason);

        return mapToEventResponse(updatedEvent);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        return mapToEventResponse(event);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventByEventId(String eventId) {
        Event event = eventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with event ID: " + eventId));

        return mapToEventResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getPublicEvents(Pageable pageable) {
        return eventRepository.findByVisibilityAndStatus(
                EventVisibility.PUBLIC, EventStatus.REGISTRATION_OPEN, pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUpcomingEvents(Pageable pageable) {
        return eventRepository.findUpcomingEvents(LocalDate.now(), EventStatus.REGISTRATION_OPEN, pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getMyEvents(Pageable pageable) {
        User currentUser = getCurrentUser();
        return eventRepository.findByOrganizer(currentUser, pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> searchEvents(String keyword, Pageable pageable) {
        return eventRepository.searchEvents(keyword, EventStatus.REGISTRATION_OPEN,
                EventVisibility.PUBLIC, pageable)
                .map(this::mapToEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getEventsByGame(String gameName, Pageable pageable) {
        return eventRepository.findByGameNameContainingIgnoreCaseAndStatus(
                gameName, EventStatus.REGISTRATION_OPEN, pageable)
                .map(this::mapToEventResponse);
    }

    // Helper methods
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String generateEventId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = eventRepository.count() + 1;
        return String.format("EVT-%s-%04d", date, count);
    }

    private void validateEventDates(LocalDate eventDate, LocalDateTime regOpenDate, LocalDateTime regCloseDate) {
        LocalDateTime now = LocalDateTime.now();

        if (eventDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Event date cannot be in the past");
        }

        if (regOpenDate.isAfter(regCloseDate)) {
            throw new BadRequestException("Registration open date must be before close date");
        }

        if (regCloseDate.isAfter(eventDate.atStartOfDay())) {
            throw new BadRequestException("Registration must close before event date");
        }
    }

    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .eventName(event.getEventName())
                .gameName(event.getGameName())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .eventStartTime(event.getEventStartTime())
                .eventEndTime(event.getEventEndTime())
                .registrationOpenDate(event.getRegistrationOpenDate())
                .registrationCloseDate(event.getRegistrationCloseDate())
                .participationType(event.getParticipationType())
                .teamSize(event.getTeamSize())
                .maxParticipants(event.getMaxParticipants())
                .minParticipants(event.getMinParticipants())
                .allowedRanks(event.getAllowedRanks())
                .platform(event.getPlatform())
                .isPaid(event.getIsPaid())
                .entryFee(event.getEntryFee())
                .prizePool(event.getPrizePool())
                .prizeDistribution(event.getPrizeDistribution())
                .refundPolicy(event.getRefundPolicy())
                .map(event.getMap())
                .mode(event.getMode())
                .serverRegion(event.getServerRegion())
                .roomId(event.getRoomId())
                .roomPassword(event.getRoomPassword())
                .rules(event.getRules())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizerName())
                .contact(event.getContact())
                .slotsFilled(event.getSlotsFilled())
                .slotsAvailable(event.getMaxParticipants() - event.getSlotsFilled())
                .status(event.getStatus())
                .thumbnailUrl(event.getThumbnailUrl())
                .liveStreamLink(event.getLiveStreamLink())
                .winnerId(event.getWinner() != null ? event.getWinner().getId() : null)
                .highlightVideoUrl(event.getHighlightVideoUrl())
                .remarks(event.getRemarks())
                .visibility(event.getVisibility())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .isRegistrationOpen(event.isRegistrationOpen())
                .build();
    }
}

