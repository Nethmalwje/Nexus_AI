package com.Nexus.Chatter.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_id", nullable = false)
    private Chatbot chatbot;

    @Column(name = "visitor_session_id", nullable = false)
    private String visitorSessionId;

    // "Haircut", "Beard trim", etc.
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    // Optional visitor details
    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_contact")
    private String customerContact;

    // Booking times (we’ll handle timezones later)
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // PENDING / CONFIRMED / CANCELLED
    @Column(nullable = false)
    private String status = "PENDING";

    // When we integrate Google, we’ll store Google event id here
    @Column(name = "google_event_id")
    private String googleEventId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Appointment(Chatbot bot, String visitorSessionId, String serviceName,
                       LocalDateTime startTime, LocalDateTime endTime) {
        this.chatbot = bot;
        this.visitorSessionId = visitorSessionId;
        this.serviceName = serviceName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
