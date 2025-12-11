package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.Chatbot;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final Client client;

    public LlmService(@Value("${google.genai.api.key}") String apiKey) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }


    public String answerQuestion(Chatbot bot, String question, String knowledgeChunkContent) {

        String systemInstruction = bot.getSystemInstruction();

        String combinedPrompt = String.format(
                "You are an AI assistant with the following system instruction:\n" +
                        "---------------------\n" +
                        "%s\n" +
                        "---------------------\n\n" +
                        "Here is some context information from the knowledge base:\n" +
                        "---------------------\n" +
                        "%s\n" +
                        "---------------------\n\n" +
                        "Using ONLY the context above (do not make up facts), answer the user's question.\n" +
                        "User question: %s",
                systemInstruction != null ? systemInstruction : "(no system instruction provided)",
                knowledgeChunkContent != null ? knowledgeChunkContent : "(no context provided)",
                question
        );

        try {
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    combinedPrompt,
                    null
            );

            return response.text();

        } catch (Exception e) {
            return "LLM error: " + e.getMessage();
        }
    }
}
