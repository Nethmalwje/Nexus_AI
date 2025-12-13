package com.Nexus.Chatter.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private final RestClient restClient;

    public OllamaService() {
        // Pointing to your local Ollama instance
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public List<Double> getEmbedding(String text) {
        // 1. Create the request payload
        // Ollama API expects: { "model": "nomic-embed-text", "prompt": "your text" }
        var request = Map.of(
                "model", "nomic-embed-text",
                "prompt", text
        );

        // 2. Call the API
        // Note: The specific endpoint for embeddings in Ollama is /api/embeddings
        Map response = restClient.post()
                .uri("/api/embeddings")
                .body(request)
                .retrieve()
                .body(Map.class);

        // 3. Extract the vector
        if (response != null && response.containsKey("embedding")) {
            return (List<Double>) response.get("embedding");
        }

        throw new RuntimeException("Ollama returned null or invalid response");
    }
}