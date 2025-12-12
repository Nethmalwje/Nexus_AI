package com.Nexus.Chatter.model;

import com.Nexus.Chatter.util.VectorConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.List;
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
     @Column(columnDefinition = "vector(768)")

     @Convert(converter = VectorConverter.class)
     @ColumnTransformer(write = "?::vector")
     private List<Double> embedding;
    // ----------------------------------------------

    @Column(name = "source_filename")
    private String sourceFilename;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}