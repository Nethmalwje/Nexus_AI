package com.Nexus.Chatter.repo;
import com.Nexus.Chatter.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;



@Repository
public interface KnowledgeChunkRepo extends JpaRepository<KnowledgeChunk, UUID> {
    // We will add a custom Native Query here later for the Vector Cosine Similarity search
    // 1. Calculate distance (<=>) between DB vector and Query vector
    // 2. Order by distance ASC (closest first)
    // 3. Limit to top results
    @Query(value = """
            SELECT * FROM knowledge_chunks 
            WHERE chatbot_id = :botId 
            ORDER BY embedding <=> CAST(:queryVector AS vector) ASC 
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeChunk> searchSimilar(
            @Param("botId") UUID botId,
            @Param("queryVector") String queryVector, // Passed as String "[0.1, 0.2...]"
            @Param("limit") int limit
    );
}