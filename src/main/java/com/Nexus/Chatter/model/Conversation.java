package com.Nexus.Chatter.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_id", nullable = false)
    private Chatbot chatbot;

    @Column(name = "visitor_session_id", nullable = false)
    private String visitorSessionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Conversation(Chatbot chatbot, String visitorSessionId) {
        this.chatbot = chatbot;
        this.visitorSessionId = visitorSessionId;
    }
}