package com.Nexus.Chatter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
// import java.util.List; // <--- Comment this out too if unused

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "knowledge_chunks")
public class KnowledgeChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_id")
    private Chatbot chatbot;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // ----------- DISABLE VECTOR FOR NOW -----------
    // @Column(columnDefinition = "vector(1536)")
    // private List<Float> embedding;
    // ----------------------------------------------

    @Column(name = "source_filename")
    private String sourceFilename;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}