package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.Chatbot;
import com.Nexus.Chatter.model.KnowledgeChunk;
import com.Nexus.Chatter.model.Tenant;
import com.Nexus.Chatter.repo.ChatbotRepo;
import com.Nexus.Chatter.repo.KnowledgeChunkRepo;
import com.Nexus.Chatter.repo.TenantRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ChatbotService {

    private final ChatbotRepo chatbotRepository;
    private final TenantRepo tenantRepository;
    private final KnowledgeChunkRepo knowledgeChunkRepository;

    // ⚡ HARDCODED TENANT ID (Nexus Corp)
    private final UUID CURRENT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public ChatbotService(ChatbotRepo chatbotRepository, TenantRepo tenantRepository, KnowledgeChunkRepo knowledgeChunkRepository) {
        this.chatbotRepository = chatbotRepository;
        this.tenantRepository = tenantRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
    }

    // 1. LIST ALL BOTS
    public List<Chatbot> getAllBots() {
        return chatbotRepository.findByTenantId(CURRENT_TENANT_ID);
    }

    // 2. CREATE BOT
    @Transactional
    public Chatbot createBot(String name, String systemInstruction) {
        Tenant tenant = tenantRepository.findById(CURRENT_TENANT_ID)
                .orElseThrow(() -> new RuntimeException("Hardcoded Tenant missing in DB!"));

        Chatbot bot = new Chatbot();
        bot.setName(name);
        bot.setSystemInstruction(systemInstruction);
        bot.setTenant(tenant); // Link to our hardcoded tenant
        bot.setTemperature(0.7); // Default

        return chatbotRepository.save(bot);
    }

    // 3. GET SINGLE BOT
    public Chatbot getBot(UUID botId) {
        return chatbotRepository.findById(botId)
                .filter(bot -> bot.getTenant().getId().equals(CURRENT_TENANT_ID)) // Safety check
                .orElseThrow(() -> new RuntimeException("Bot not found"));
    }
}

