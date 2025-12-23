package com.Nexus.Chatter.repo;

import com.Nexus.Chatter.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepo extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByChatbotIdOrderByStartTimeDesc(UUID botId);
    List<Appointment> findByChatbotIdAndVisitorSessionIdOrderByStartTimeDesc(UUID botId, String visitorSessionId);

    // ✅ Overlap check for CONFIRMED appointments
    // Overlap logic: existing.start < newEnd AND existing.end > newStart
    @Query("""
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.chatbot.id = :botId
          AND a.status = 'CONFIRMED'
          AND a.startTime < :endTime
          AND a.endTime > :startTime
    """)
    boolean hasConfirmedOverlap(
            @Param("botId") UUID botId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ✅ Cluster-safe lock per bot (Postgres)
    // Locks within the current transaction until commit/rollback
    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
    void lockForBot(@Param("lockKey") String lockKey);

}
