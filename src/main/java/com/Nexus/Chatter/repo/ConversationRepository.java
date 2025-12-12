package com.Nexus.Chatter.repo;

import com.Nexus.Chatter.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    // Find a specific session for a specific bot
    Optional<Conversation> findByChatbotIdAndVisitorSessionId(UUID chatbotId, String visitorSessionId);
}