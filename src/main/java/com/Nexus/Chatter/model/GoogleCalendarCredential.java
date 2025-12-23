package com.Nexus.Chatter.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "google_calendar_credentials")
public class GoogleCalendarCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // One bot has one Google connection (simple approach)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_id", nullable = false, unique = true)
    private Chatbot chatbot;

    @Column(name = "refresh_token", columnDefinition = "TEXT", nullable = false)
    private String refreshToken;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "scope")
    private String scope;

    @Column(name = "token_type")
    private String tokenType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
