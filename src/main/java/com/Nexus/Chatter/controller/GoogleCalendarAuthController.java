package com.Nexus.Chatter.controller;
import com.Nexus.Chatter.repo.GoogleCalendarCredentialRepo;

import com.Nexus.Chatter.service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.Nexus.Chatter.service.GoogleCalendarService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/google")
public class GoogleCalendarAuthController {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    private final GoogleOAuthService googleOAuthService;

    private final GoogleCalendarService googleCalendarService;
    private final GoogleCalendarCredentialRepo credentialRepo;



    public GoogleCalendarAuthController(GoogleOAuthService googleOAuthService, GoogleCalendarService googleCalendarService, GoogleCalendarCredentialRepo credentialRepo) {
        this.googleOAuthService = googleOAuthService;
        this.googleCalendarService = googleCalendarService;
        this.credentialRepo = credentialRepo;
    }

    // 2.5 — redirect to Google consent screen
    @GetMapping("/connect/{botId}")
    public void connect(@PathVariable UUID botId, HttpServletResponse response) throws IOException {

        String scopeRaw =
                "https://www.googleapis.com/auth/calendar.events " +
                        "https://www.googleapis.com/auth/calendar.readonly";

        String scope = URLEncoder.encode(scopeRaw, StandardCharsets.UTF_8);
        String state = botId.toString(); // remember botId

        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope=" + scope
                        + "&access_type=offline"
                        + "&prompt=consent"
                        + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        response.sendRedirect(url);
    }

    // 2.6 — exchange code -> tokens, store in DB
    @GetMapping("/callback")
    public Map<String, Object> callback(@RequestParam("code") String code,
                                        @RequestParam("state") String state) {

        UUID botId = UUID.fromString(state);

        googleOAuthService.exchangeCodeAndStore(botId, code);

        return Map.of(
                "status", "CONNECTED",
                "botId", botId.toString(),
                "message", "Tokens stored. Next: call /api/v1/google/status/{botId} to verify."
        );
    }

    @GetMapping("/status/{botId}")
    public Map<String, Object> status(@PathVariable UUID botId) {

        var cred = credentialRepo.findByChatbotId(botId)
                .orElseThrow(() -> new RuntimeException("No Google credentials found for bot"));

        String accessToken = googleCalendarService.getValidAccessToken(botId);

        return Map.of(
                "status", "OK",
                "botId", botId.toString(),
                "accessTokenPresent", accessToken != null && !accessToken.isBlank(),
                "scope", cred.getScope(),
                "expiresAt", cred.getExpiresAt(),
                "tokenType", cred.getTokenType()
        );
    }


}
