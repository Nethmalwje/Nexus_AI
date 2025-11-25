package com.Nexus.Chatter.service;
import com.Nexus.Chatter.model.Chatbot;
import com.Nexus.Chatter.model.KnowledgeChunk;
import com.Nexus.Chatter.repo.ChatbotRepo;
import com.Nexus.Chatter.repo.KnowledgeChunkRepo;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentService {

    private final ChatbotRepo chatbotRepository;
    private final KnowledgeChunkRepo knowledgeChunkRepository;
    private final Tika tika;

    public DocumentService(ChatbotRepo chatbotRepository, KnowledgeChunkRepo knowledgeChunkRepository) {
        this.chatbotRepository = chatbotRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.tika = new Tika(); // Automatically handles PDF, DOCX, TXT
    }

    @Transactional
    public String ingestFile(UUID botId, MultipartFile file) throws IOException, TikaException {
        // 1. Validate Bot
        Chatbot bot = chatbotRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found"));

        // 2. Extract Text
        String content = tika.parseToString(file.getInputStream());
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("File was empty");
        }

        // 3. Simple Chunking (Split by 500 chars)
        // In real life, we use more advanced splitters, but this works for v1
        String[] chunks = content.split("(?<=\\G.{500})");

        // 4. Save to DB
        for (String chunkText : chunks) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setChatbot(bot);
            chunk.setSourceFilename(file.getOriginalFilename());
            chunk.setContent(chunkText);
            // chunk.setEmbedding(...) -> Disabled until later

            knowledgeChunkRepository.save(chunk);
        }

        return "Successfully processed " + chunks.length + " chunks from " + file.getOriginalFilename();
    }
}