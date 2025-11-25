package com.Nexus.Chatter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "chatbots")
public class Chatbot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    @JsonIgnore
    private Tenant tenant;

    @Column(name = "public_id", unique = true)
    private UUID publicId = UUID.randomUUID(); // Public safe ID

    @Column(nullable = false)
    private String name;

    @Column(name = "system_instruction", columnDefinition = "TEXT")
    private String systemInstruction;

    private Double temperature = 0.7;

    @Column(name = "primary_color")
    private String primaryColor = "#000000";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}