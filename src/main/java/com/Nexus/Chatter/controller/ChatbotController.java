package com.Nexus.Chatter.controller;

import com.Nexus.Chatter.model.Chatbot;
import com.Nexus.Chatter.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bots") // Simple URL structure
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/")
    public ResponseEntity<List<Chatbot>> getBots() {
        return ResponseEntity.ok(chatbotService.getAllBots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Chatbot> getBot(@PathVariable UUID id) {
        return ResponseEntity.ok(chatbotService.getBot(id));
    }

    @PostMapping("/")
    public ResponseEntity<Chatbot> createBot(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String instruction = payload.get("systemInstruction");
        return ResponseEntity.ok(chatbotService.createBot(name, instruction));
    }

    // ⭐ NEW
    @PostMapping("/{botId}/ask")
    public ResponseEntity<Map<String, String>> askBot(
            @PathVariable UUID botId,
            @RequestBody Map<String, String> body
    ) {
        // Expecting: { "question": "..." }
        String question = body.get("question");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "QUESTION_MISSING"));
        }

        String answer = chatbotService.answerQuestion(botId, question);

        // Respond as: { "answer": "..." }
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}