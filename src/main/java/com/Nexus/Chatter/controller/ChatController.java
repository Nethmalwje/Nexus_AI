package com.Nexus.Chatter.controller;


import com.Nexus.Chatter.service.ChatService;
import com.Nexus.Chatter.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final SearchService searchService;

//    public ChatController(SearchService searchService) {
//        this.searchService = searchService;
//    }
    private final ChatService chatService;

//    public ChatController(ChatService chatService) {
//        this.chatService = chatService;
//    }
public ChatController(SearchService searchService, ChatService chatService) {
    this.searchService = searchService;
    this.chatService = chatService;
}
    // POST http://localhost:8080/api/v1/chat/{botId}/search
    @PostMapping("/{botId}/search")
    public List<String> search(@PathVariable UUID botId, @RequestBody Map<String, String> payload) {
        String query = payload.get("query");
        return searchService.search(botId, query);
    }

//    @PostMapping("/{botId}/ask")
//    public Map<String, String> ask(@PathVariable UUID botId, @RequestBody Map<String, String> payload) {
//        String query = payload.get("query");
//        String answer = chatService.generateResponse(botId, query);
//        return Map.of("answer", answer);
//    }
@PostMapping("/{botId}/ask")
public Map<String, String> ask(@PathVariable UUID botId, @RequestBody Map<String, String> payload) {
    String query = payload.get("query");

    // Frontend should send this! If null, generate a temporary one for testing.
    String visitorId = payload.getOrDefault("visitorId", "test-user-123");

    String answer = chatService.generateResponse(botId, visitorId, query);
    return Map.of("answer", answer);
}
}