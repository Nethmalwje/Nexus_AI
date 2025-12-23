package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.Chatbot;
import com.Nexus.Chatter.model.GoogleCalendarCredential;
import com.Nexus.Chatter.repo.ChatbotRepo;
import com.Nexus.Chatter.repo.GoogleCalendarCredentialRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleOAuthService {

    private final RestClient restClient;
    private final ChatbotRepo chatbotRepo;
    private final GoogleCalendarCredentialRepo credentialRepo;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    public GoogleOAuthService(ChatbotRepo chatbotRepo,
                              GoogleCalendarCredentialRepo credentialRepo) {
        this.chatbotRepo = chatbotRepo;
        this.credentialRepo = credentialRepo;

        this.restClient = RestClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .build();
    }

    @Transactional
    public void exchangeCodeAndStore(UUID botId, String code) {

        Chatbot bot = chatbotRepo.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + botId));

        // Google token exchange requires x-www-form-urlencoded
        Map<String, String> form = Map.of(
                "code", code,
                "client_id", clientId,
                "client_secret", clientSecret,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"
        );

        Map response = restClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(toFormBody(form))
                .retrieve()
                .body(Map.class);

        if (response == null) throw new RuntimeException("Token response is null");

        String accessToken = (String) response.get("access_token");
        String refreshToken = (String) response.get("refresh_token"); // ⚠️ may be null if user already approved before
        String scope = (String) response.get("scope");
        String tokenType = (String) response.get("token_type");

        Integer expiresIn = null;
        Object expObj = response.get("expires_in");
        if (expObj instanceof Number n) expiresIn = n.intValue();

        LocalDateTime expiresAt = (expiresIn != null)
                ? LocalDateTime.now().plusSeconds(expiresIn)
                : null;

        // Upsert by botId
        GoogleCalendarCredential cred = credentialRepo.findByChatbotId(botId)
                .orElseGet(GoogleCalendarCredential::new);

        cred.setChatbot(bot);
        cred.setAccessToken(accessToken);
        cred.setScope(scope);
        cred.setTokenType(tokenType);
        cred.setExpiresAt(expiresAt);
        cred.setUpdatedAt(LocalDateTime.now());

        // ✅ refresh token only appears first time unless prompt=consent was used
        if (refreshToken != null && !refreshToken.isBlank()) {
            cred.setRefreshToken(refreshToken);
        } else if (cred.getRefreshToken() == null) {
            throw new RuntimeException("""
                    Google did not return a refresh_token.
                    Fix: add prompt=consent & access_type=offline in Step 2.5 connect URL,
                    or revoke access in Google account and re-connect.
                    """);
        }

        credentialRepo.save(cred);
    }

    // Convert Map to "a=b&c=d" for x-www-form-urlencoded
    private String toFormBody(Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(urlEncode(e.getKey())).append("=").append(urlEncode(e.getValue()));
        }
        return sb.toString();
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
