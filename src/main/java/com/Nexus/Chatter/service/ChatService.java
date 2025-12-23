package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.Chatbot;
import com.Nexus.Chatter.model.ChatMessage;
import com.Nexus.Chatter.model.Conversation;
import com.Nexus.Chatter.repo.ChatMessageRepository;
import com.Nexus.Chatter.repo.ChatbotRepo;
import com.Nexus.Chatter.repo.ConversationRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
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
    private final AppointmentService appointmentService;

    private final RestClient geminiClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    public ChatService(SearchService searchService,
                       ChatbotRepo chatbotRepository,
                       ConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       AppointmentService appointmentService) {

        this.searchService = searchService;
        this.chatbotRepository = chatbotRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.appointmentService = appointmentService;

        this.geminiClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private LocalDateTime parseFlexibleDateTime(String s) {
        if (s == null) throw new IllegalArgumentException("startTime/endTime is null");

        String v = s.trim();

        // Accept: "2025-10-20 14:00" -> "2025-10-20T14:00"
        if (v.contains(" ") && !v.contains("T")) {
            v = v.replace(" ", "T");
        }

        // Accept: "2025-10-20T14:00" -> add seconds
        if (v.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
            v = v + ":00";
        }

        return LocalDateTime.parse(v); // now ISO-friendly
    }


    private String extractFirstJsonObject(String text) {
        if (text == null) return "{}";

        // Remove ```json ... ``` or ``` ... ```
        text = text.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) text = text.substring(firstNewline + 1);
            int lastFence = text.lastIndexOf("```");
            if (lastFence >= 0) text = text.substring(0, lastFence);
            text = text.trim();
        }

        // Find first '{' and match until its closing '}'
        int start = text.indexOf('{');
        if (start < 0) return "{}";

        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '{') depth++;
            if (ch == '}') depth--;
            if (depth == 0) {
                return text.substring(start, i + 1).trim();
            }
        }
        return "{}";
    }


    public String generateResponse(UUID botId, String visitorId, String userQuestion) {

        Chatbot bot = chatbotRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found"));

        Conversation conversation = conversationRepository
                .findByChatbotIdAndVisitorSessionId(botId, visitorId)
                .orElseGet(() -> conversationRepository.save(new Conversation(bot, visitorId)));

        messageRepository.save(new ChatMessage(conversation, "USER", userQuestion));

        List<ChatMessage> recentMsgs =
                messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(conversation.getId());
        Collections.reverse(recentMsgs);

        StringBuilder history = new StringBuilder();
        for (ChatMessage msg : recentMsgs) {
            history.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
        }

        // --------------------------------------------------
        // PHASE 5 — SAFE INTENT ROUTING
        // --------------------------------------------------
        BookingIntent intent = extractBookingIntent(bot, userQuestion, history.toString());
        String intentType = intent.intent == null ? "" : intent.intent.trim().toUpperCase();
        // ✅ Ask follow-up ONLY if it REALLY looks like a booking attempt
        if ("NONE".equals(intentType)
                && intent.confidence != null
                && intent.confidence >= 0.7
                && intent.followUpQuestion != null) {

            saveBot(conversation, intent.followUpQuestion);
            return intent.followUpQuestion;
        }

        // ✅ Booking flow
        if ("BOOK_APPOINTMENT".equals(intentType)
                && intent.confidence != null
                && intent.confidence >= 0.7) {

            LocalDateTime start = parseFlexibleDateTime(intent.startTime);
            LocalDateTime end = parseFlexibleDateTime(intent.endTime);


            var appt = appointmentService.createAppointment(
                    botId,
                    visitorId,
                    intent.serviceName,
                    start,
                    end,
                    intent.customerName,
                    intent.customerContact
            );

            var result = appointmentService.confirmAppointment(appt.getId());

            String reply = result.ok
                    ? "✅ Booking confirmed!\nService: " + appt.getServiceName()
                    + "\nStart: " + appt.getStartTime()
                    + "\nEnd: " + appt.getEndTime()
                    : result.httpStatus == 409
                    ? "❌ That time slot is not available. Please choose another."
                    : "❌ Booking failed: " + result.message;

            saveBot(conversation, reply);
            return reply;
        }

        // --------------------------------------------------
        // NORMAL RAG CHAT (DEFAULT PATH)
        // --------------------------------------------------
        List<String> relatedDocs = searchService.search(botId, userQuestion);
        String ragContext = String.join("\n---\n", relatedDocs);

        String prompt = """
                You are a helpful AI assistant.

                HISTORY:
                %s

                DOCUMENTS:
                %s

                QUESTION:
                %s
                """.formatted(history, ragContext, userQuestion);

        String ai = callGemini(prompt);
        saveBot(conversation, ai);
        return ai;
    }

    private void saveBot(Conversation c, String msg) {
        messageRepository.save(new ChatMessage(c, "BOT", msg));
    }

    // --------------------------------------------------
    // INTENT EXTRACTION
    // --------------------------------------------------
    private static class BookingIntent {
        public String intent;
        public String serviceName;
        public String startTime;
        public String endTime;
        public String customerName;
        public String customerContact;
        public String followUpQuestion;
        public Double confidence;
    }

    private BookingIntent extractBookingIntent(Chatbot bot,
                                               String userMessage,
                                               String historyText) {

        String prompt = """
You are an appointment booking intent extractor.

Return ONLY valid JSON.

{
  "intent": "BOOK_APPOINTMENT" | "NONE",
  "serviceName": string | null,
  "startTime": string | null,
  "endTime": string | null,
  "customerName": string | null,
  "customerContact": string | null,
  "confidence": number,
  "followUpQuestion": string | null
}

Rules:
- If the user is NOT trying to book, set intent="NONE", confidence < 0.3, followUpQuestion=null
- If booking intent exists but details missing, confidence >= 0.7 and ask followUpQuestion
- Do NOT guess dates or times

Business: %s
Instructions: %s

History:
%s

Message:
%s
""".formatted(
                bot.getName(),
                bot.getSystemInstruction() == null ? "" : bot.getSystemInstruction(),
                historyText,
                userMessage
        );

        try {
            String raw = callGemini(prompt);
            String cleanJson = extractFirstJsonObject(raw);   // ✅ new helper
            return objectMapper.readValue(cleanJson, BookingIntent.class);

        } catch (Exception e) {
            BookingIntent bi = new BookingIntent();
            bi.intent = "NONE";
            bi.confidence = 0.0;
            bi.followUpQuestion = null;
            return bi;
        }
    }

    private String callGemini(String prompt) {
        var body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        Map res = geminiClient.post()
                .uri(u -> u.path("/v1beta/models/gemini-2.5-flash:generateContent")
                        .queryParam("key", apiKey).build())
                .body(body)
                .retrieve()
                .body(Map.class);

        List c = (List) res.get("candidates");
        Map p = (Map) ((Map) c.get(0)).get("content");
        return (String) ((Map) ((List) p.get("parts")).get(0)).get("text");
    }
}
