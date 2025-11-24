package com.Nexus.Chatter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "chatbot_id")
    private Chatbot chatbot;

    @Column(name = "visitor_session_id")
    private String visitorSessionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

// ... Create Message.java similarly ...