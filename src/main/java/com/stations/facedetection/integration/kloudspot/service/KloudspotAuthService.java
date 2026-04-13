package com.stations.facedetection.integration.kloudspot.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stations.facedetection.integration.kloudspot.config.KloudspotProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KloudspotAuthService {

    private final WebClient webClient;
    private final KloudspotProperties properties;
    private final ObjectMapper objectMapper; // ✅ Injected

    public String getToken() {

        String url = properties.getBaseUrl() + properties.getAuthUrl();

        log.info("Requesting JWT token from Kloudspot API: {}", url);

        Map<String, String> body = new HashMap<>();
        body.put("id", properties.getId());
        body.put("secretKey", properties.getSecretKey());

        try {

            String response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                throw new RuntimeException("Empty response from Kloudspot auth API");
            }

            log.debug("Raw auth response received");

            String token = extractToken(response);

            log.info("Kloudspot JWT token generated successfully (length={})", token.length());

            return token;

        } catch (Exception ex) {
            log.error("Failed to generate JWT token from Kloudspot API", ex);
            throw new RuntimeException("Kloudspot authentication failed", ex);
        }
    }

    private String extractToken(String response) {

        try {
            // Try JSON parsing
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("token")) {
                return jsonNode.get("token").asText().trim();
            }

        } catch (Exception ignored) {
            // Not JSON → ignore
        }

        // Plain string fallback
        return response.replace("\"", "").trim();
    }
}