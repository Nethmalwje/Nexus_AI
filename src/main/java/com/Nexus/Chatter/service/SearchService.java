package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.KnowledgeChunk;
import com.Nexus.Chatter.repo.KnowledgeChunkRepo;
import com.Nexus.Chatter.util.VectorConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final OllamaService ollamaService;
    private final KnowledgeChunkRepo chunkRepository;
    private final VectorConverter vectorConverter;

    public SearchService(OllamaService ollamaService,
                         KnowledgeChunkRepo chunkRepository) {
        this.ollamaService = ollamaService;
        this.chunkRepository = chunkRepository;
        this.vectorConverter = new VectorConverter(); // Helper to format the vector
    }

    public List<String> search(UUID botId, String queryText) {
        // 1. Convert User's Question -> Vector
        List<Double> queryEmbedding = ollamaService.getEmbedding(queryText);

        // 2. Convert List<Double> -> String format "[0.1, 0.2...]" for SQL
        String vectorString = vectorConverter.convertToDatabaseColumn(queryEmbedding);

        // 3. Run the Magic SQL Query
        List<KnowledgeChunk> results = chunkRepository.searchSimilar(botId, vectorString, 3);

        // 4. Extract just the text content to return
        return results.stream()
                .map(KnowledgeChunk::getContent)
                .collect(Collectors.toList());
    }
}