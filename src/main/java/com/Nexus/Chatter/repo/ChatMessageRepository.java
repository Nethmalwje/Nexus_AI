package com.Nexus.Chatter.repo;

import com.Nexus.Chatter.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    // Fetch the NEWEST 10 messages for context (Sliding Window)
    List<ChatMessage> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
    List<ChatMessage> findTop3ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}