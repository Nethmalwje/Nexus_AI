package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.GoogleCalendarCredential;
import com.Nexus.Chatter.repo.GoogleCalendarCredentialRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleCalendarService {

    private final GoogleCalendarCredentialRepo credentialRepo;

    private final RestClient oauthClient;
    private final RestClient calendarClient;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    private static final ZoneId ZONE = ZoneId.of("Asia/Colombo");
    private static final DateTimeFormatter RFC3339 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public GoogleCalendarService(GoogleCalendarCredentialRepo credentialRepo) {
        this.credentialRepo = credentialRepo;

        this.oauthClient = RestClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .build();

        this.calendarClient = RestClient.builder()
                .baseUrl("https://www.googleapis.com")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // -----------------------------
    // Step 2.7 - Token refresh logic
    // -----------------------------
    @Transactional
    public String getValidAccessToken(UUID botId) {
        GoogleCalendarCredential cred = credentialRepo.findByChatbotId(botId)
                .orElseThrow(() -> new RuntimeException("Google not connected for botId=" + botId));

        if (cred.getAccessToken() != null && cred.getExpiresAt() != null) {
            if (cred.getExpiresAt().isAfter(LocalDateTime.now().plusSeconds(30))) {
                return cred.getAccessToken();
            }
        }

        if (cred.getRefreshToken() == null || cred.getRefreshToken().isBlank()) {
            throw new RuntimeException("Missing refresh_token. Reconnect using /connect/{botId} with prompt=consent.");
        }

        String formBody =
                "client_id=" + urlEncode(clientId)
                        + "&client_secret=" + urlEncode(clientSecret)
                        + "&refresh_token=" + urlEncode(cred.getRefreshToken())
                        + "&grant_type=refresh_token";

        Map tokenResponse = oauthClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body(Map.class);

        if (tokenResponse == null) throw new RuntimeException("Refresh token response is null");

        String newAccessToken = (String) tokenResponse.get("access_token");
        if (newAccessToken == null || newAccessToken.isBlank()) {
            throw new RuntimeException("Refresh did not return access_token. Response=" + tokenResponse);
        }

        Object expObj = tokenResponse.get("expires_in");
        Integer expiresIn = null;
        if (expObj instanceof Number n) expiresIn = n.intValue();

        LocalDateTime newExpiresAt = (expiresIn != null)
                ? LocalDateTime.now().plusSeconds(expiresIn)
                : null;

        cred.setAccessToken(newAccessToken);
        cred.setExpiresAt(newExpiresAt);
        cred.setUpdatedAt(LocalDateTime.now());
        credentialRepo.save(cred);

        return newAccessToken;
    }

    // -----------------------------
    // Phase 3.2 - FreeBusy
    // -----------------------------
    public Map freeBusy(UUID botId, LocalDateTime startTime, LocalDateTime endTime) {
        String accessToken = getValidAccessToken(botId);

        ZonedDateTime startZ = startTime.atZone(ZONE);
        ZonedDateTime endZ = endTime.atZone(ZONE);

        String timeMin = startZ.toOffsetDateTime().format(RFC3339);
        String timeMax = endZ.toOffsetDateTime().format(RFC3339);

        Map<String, Object> requestBody = Map.of(
                "timeMin", timeMin,
                "timeMax", timeMax,
                "items", List.of(Map.of("id", "primary"))
        );

        try {
            return calendarClient.post()
                    .uri("/calendar/v3/freeBusy")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(
                    "FreeBusy failed: " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    public boolean isSlotAvailable(UUID botId, LocalDateTime startTime, LocalDateTime endTime) {
        Map resp = freeBusy(botId, startTime, endTime);

        if (resp == null) throw new RuntimeException("FreeBusy response is null");

        Object calendarsObj = resp.get("calendars");
        if (!(calendarsObj instanceof Map calendars)) return true;

        Object primaryObj = calendars.get("primary");
        if (!(primaryObj instanceof Map primary)) return true;

        Object busyObj = primary.get("busy");
        if (!(busyObj instanceof List busyList)) return true;

        return busyList.isEmpty();
    }

    // -----------------------------
    // Phase 3 - Create event
    // -----------------------------
    public String createEvent(UUID botId,
                              String summary,
                              String description,
                              LocalDateTime startTime,
                              LocalDateTime endTime) {

        String accessToken = getValidAccessToken(botId);

        ZonedDateTime startZ = startTime.atZone(ZONE);
        ZonedDateTime endZ = endTime.atZone(ZONE);

        String startStr = startZ.toOffsetDateTime().format(RFC3339);
        String endStr = endZ.toOffsetDateTime().format(RFC3339);

        Map<String, Object> eventBody = Map.of(
                "summary", summary,
                "description", description,
                "start", Map.of(
                        "dateTime", startStr,
                        "timeZone", ZONE.getId()
                ),
                "end", Map.of(
                        "dateTime", endStr,
                        "timeZone", ZONE.getId()
                )
        );

        try {
            Map response = calendarClient.post()
                    .uri("/calendar/v3/calendars/primary/events")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("id")) {
                throw new RuntimeException("Google event create failed. Response=" + response);
            }

            return (String) response.get("id");
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(
                    "CreateEvent failed: " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
