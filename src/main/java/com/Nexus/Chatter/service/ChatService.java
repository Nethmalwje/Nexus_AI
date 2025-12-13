////package com.Nexus.Chatter.service;
////
////
////import org.springframework.stereotype.Service;
////import org.springframework.web.client.RestClient;
////import java.util.List;
////import java.util.Map;
////import java.util.UUID;
////
////@Service
////public class ChatService {
////
////    private final SearchService searchService; // 1. Dependency on Search
////    private final RestClient chatClient;       // 2. Client for the LLM
////
////    public ChatService(SearchService searchService) {
////        this.searchService = searchService;
////        // Setup client for Ollama Generation
////        this.chatClient = RestClient.builder()
////                .baseUrl("http://localhost:11434")
////                .build();
////    }
////
////    public String generateResponse(UUID botId, String userQuestion) {
////        // Step A: RETRIEVE (Get the context)
////        List<String> relatedDocs = searchService.search(botId, userQuestion);
////
////        // Step B: AUGMENT (Build the Prompt)
////        String context = String.join("\n---\n", relatedDocs);
////        String prompt = """
////                You are a helpful AI assistant. Answer the question based strictly on the context below.
////
////                CONTEXT:
////                %s
////
////                QUESTION:
////                %s
////                """.formatted(context, userQuestion);
////
////        // Step C: GENERATE (Call the LLM)
////        // Ensure you have run: `ollama pull llama3`
//////        var request = Map.of(
//////                "model", "llama3",
//////                "prompt", prompt,
//////                "stream", false
//////        );
////        var request = Map.of(
////                "model", "tinyllama", // <--- Update this name
////                "prompt", prompt,
////                "stream", false
////        );
////
////        Map response = chatClient.post()
////                .uri("/api/generate")
////                .body(request)
////                .retrieve()
////                .body(Map.class);
////
////        if (response != null && response.containsKey("response")) {
////            return (String) response.get("response");
////        }
////
////        return "I'm sorry, I couldn't generate a response.";
////    }
////}
//
//package com.Nexus.Chatter.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestClient;
//
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//@Service
//public class ChatService {
//
//    private final SearchService searchService;
//    private final RestClient geminiClient;
//
//    // Inject key from application.properties
//    @Value("${gemini.api.key}")
//    private String apiKey;
//
//    public ChatService(SearchService searchService) {
//        this.searchService = searchService;
//        // 1. SWAP: Change Base URL to Google
//        this.geminiClient = RestClient.builder()
//                .baseUrl("https://generativelanguage.googleapis.com")
//                .defaultHeader("Content-Type", "application/json")
//                .build();
//    }
//
//    public String generateResponse(UUID botId, String userQuestion) {
//        // Step A: RETRIEVE (This part stays EXACTLY the same!)
//        List<String> relatedDocs = searchService.search(botId, userQuestion);
//
//        // Step B: AUGMENT (Same prompt logic)
//        String context = String.join("\n---\n", relatedDocs);
//        String prompt = """
//                You are a helpful AI assistant. Answer the question based strictly on the context below.
//                CONTEXT:
//                %s
//                QUESTION:
//                %s
//                """.formatted(context, userQuestion);
//
//        // Step C: GENERATE (This is the "Swap" part)
//
//        // 2. SWAP: Translate "prompt" into Gemini's specific JSON structure
//        var requestBody = Map.of(
//                "contents", List.of(
//                        Map.of("parts", List.of(
//                                Map.of("text", prompt)
//                        ))
//                )
//        );
//
//        // 3. SWAP: Send to Gemini Endpoint
//        Map response = geminiClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/v1beta/models/gemini-2.5-flash:generateContent")
//                        .queryParam("key", apiKey) // Google needs key in URL
//                        .build())
//                .body(requestBody)
//                .retrieve()
//                .body(Map.class);
//
//        // 4. SWAP: Unpack Gemini's complex response
//        try {
//            if (response != null && response.containsKey("candidates")) {
//                List candidates = (List) response.get("candidates");
//                Map firstCandidate = (Map) candidates.get(0);
//                Map content = (Map) firstCandidate.get("content");
//                List parts = (List) content.get("parts");
//                Map firstPart = (Map) parts.get(0);
//                return (String) firstPart.get("text");
//            }
//        } catch (Exception e) {
//            return "Error parsing Gemini response: " + e.getMessage();
//        }
//
//        return "I'm sorry, I couldn't generate a response.";
//    }
//}
package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.Chatbot;
import com.Nexus.Chatter.model.ChatMessage;
import com.Nexus.Chatter.model.Conversation;
import com.Nexus.Chatter.repo.ChatMessageRepository;
import com.Nexus.Chatter.repo.ChatbotRepo;
import com.Nexus.Chatter.repo.ChatbotRepo;
import com.Nexus.Chatter.repo.ConversationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatService {

    private final SearchService searchService;
    private final ChatbotRepo chatbotRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final RestClient geminiClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    public ChatService(SearchService searchService,
                       ChatbotRepo chatbotRepository,
                       ConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository) {
        this.searchService = searchService;
        this.chatbotRepository = chatbotRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;

        this.geminiClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String generateResponse(UUID botId, String visitorId, String userQuestion) {

        // 1. Load Bot
        Chatbot bot = chatbotRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found"));

        // 2. Get or Create Session
        Conversation conversation = conversationRepository
                .findByChatbotIdAndVisitorSessionId(botId, visitorId)
                .orElseGet(() -> conversationRepository.save(new Conversation(bot, visitorId)));

        // 3. Save USER Message to DB (So we remember it next time)
        messageRepository.save(new ChatMessage(conversation, "USER", userQuestion));

        // 4. Load History (Context)
        // We get top 10 NEWEST, so we must reverse them to be chronological (Oldest -> Newest)
        List<ChatMessage> recentMsgs = messageRepository.findTop3ByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Collections.reverse(recentMsgs);

        // Build History String
        StringBuilder history = new StringBuilder();
        for (ChatMessage msg : recentMsgs) {
            history.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
        }

        // 5. RAG Retrieval
        List<String> relatedDocs = searchService.search(botId, userQuestion);
        String ragContext = String.join("\n---\n", relatedDocs);

        // 6. Build Prompt with Memory AND Documents
        String prompt = """
                You are a helpful AI assistant.
                
                HISTORY OF CONVERSATION:
                %s
                
                RELEVANT DOCUMENTS:
                %s
                
                USER QUESTION:
                %s
                """.formatted(history.toString(), ragContext, userQuestion);

        // 7. Call Gemini
        String aiResponse = callGemini(prompt);

        // 8. Save BOT Response to DB
        messageRepository.save(new ChatMessage(conversation, "BOT", aiResponse));

        return aiResponse;
    }

    private String callGemini(String prompt) {
        var requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            Map response = geminiClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/gemini-2.5-flash:generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                List parts = (List) content.get("parts");
                Map firstPart = (Map) parts.get(0);
                return (String) firstPart.get("text");
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        return "I'm sorry, I couldn't generate a response.";
    }
}