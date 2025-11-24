package com.Nexus.Chatter.repo;
import com.Nexus.Chatter.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;



@Repository
public interface KnowledgeChunkRepo extends JpaRepository<KnowledgeChunk, UUID> {
    // We will add a custom Native Query here later for the Vector Cosine Similarity search
}