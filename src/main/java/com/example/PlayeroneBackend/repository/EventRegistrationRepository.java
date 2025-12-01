package com.example.PlayeroneBackend.repository;

import com.example.PlayeroneBackend.entity.Event;
import com.example.PlayeroneBackend.entity.EventRegistration;
import com.example.PlayeroneBackend.entity.User;
import com.example.PlayeroneBackend.enums.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    // Check if user already registered for event
    boolean existsByEventAndUser(Event event, User user);

    // Find registration by event and user
    Optional<EventRegistration> findByEventAndUser(Event event, User user);

    // Find all registrations for an event
    Page<EventRegistration> findByEvent(Event event, Pageable pageable);

    List<EventRegistration> findByEvent(Event event);

    // Find all registrations by user
    Page<EventRegistration> findByUser(User user, Pageable pageable);

    List<EventRegistration> findByUserId(Long userId);

    // Find registrations by status
    List<EventRegistration> findByEventAndStatus(Event event, RegistrationStatus status);

    // Count registrations by event
    long countByEvent(Event event);

    // Count registrations by event and status
    long countByEventAndStatus(Event event, RegistrationStatus status);

    // Find confirmed registrations for event
    @Query("SELECT r FROM EventRegistration r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    List<EventRegistration> findConfirmedRegistrationsByEventId(@Param("eventId") Long eventId);

    // Get user's upcoming event registrations
    @Query("SELECT r FROM EventRegistration r WHERE r.user.id = :userId AND " +
           "r.event.eventDate >= CURRENT_DATE AND r.status IN ('PENDING', 'CONFIRMED')")
    List<EventRegistration> findUpcomingRegistrationsByUserId(@Param("userId") Long userId);
}

