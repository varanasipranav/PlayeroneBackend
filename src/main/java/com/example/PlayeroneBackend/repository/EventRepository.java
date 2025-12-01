package com.example.PlayeroneBackend.repository;

import com.example.PlayeroneBackend.entity.Event;
import com.example.PlayeroneBackend.entity.User;
import com.example.PlayeroneBackend.enums.EventStatus;
import com.example.PlayeroneBackend.enums.EventVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    // Find events by organizer
    Page<Event> findByOrganizer(User organizer, Pageable pageable);

    List<Event> findByOrganizerId(Long organizerId);

    // Find by status
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    // Find public events
    Page<Event> findByVisibilityAndStatus(EventVisibility visibility, EventStatus status, Pageable pageable);

    // Find upcoming events
    @Query("SELECT e FROM Event e WHERE e.eventDate >= :today AND e.status = :status ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingEvents(@Param("today") LocalDate today, @Param("status") EventStatus status, Pageable pageable);

    // Find events by game name
    Page<Event> findByGameNameContainingIgnoreCaseAndStatus(String gameName, EventStatus status, Pageable pageable);

    // Search events
    @Query("SELECT e FROM Event e WHERE " +
           "(LOWER(e.eventName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.gameName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "e.status = :status AND e.visibility = :visibility")
    Page<Event> searchEvents(@Param("keyword") String keyword,
                             @Param("status") EventStatus status,
                             @Param("visibility") EventVisibility visibility,
                             Pageable pageable);

    // Find events with open registration
    @Query("SELECT e FROM Event e WHERE e.status = 'REGISTRATION_OPEN' AND " +
           "e.registrationOpenDate <= :now AND e.registrationCloseDate > :now AND " +
           "e.slotsFilled < e.maxParticipants")
    Page<Event> findEventsWithOpenRegistration(@Param("now") LocalDateTime now, Pageable pageable);

    // Count events by organizer
    long countByOrganizer(User organizer);

    // Find events by date range
    @Query("SELECT e FROM Event e WHERE e.eventDate BETWEEN :startDate AND :endDate")
    List<Event> findEventsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}


