package com.Nexus.Chatter.repo;

import com.Nexus.Chatter.model.GoogleCalendarCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleCalendarCredentialRepo extends JpaRepository<GoogleCalendarCredential, UUID> {
    Optional<GoogleCalendarCredential> findByChatbotId(UUID chatbotId);
}
