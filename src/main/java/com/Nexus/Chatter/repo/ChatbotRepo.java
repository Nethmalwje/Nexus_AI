package com.Nexus.Chatter.repo;


import com.Nexus.Chatter.model.Chatbot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;


@Repository
public interface ChatbotRepo extends JpaRepository<Chatbot, UUID> {
    List<Chatbot> findByTenantId(UUID tenantId);
    Chatbot findByPublicId(UUID publicId);
}

