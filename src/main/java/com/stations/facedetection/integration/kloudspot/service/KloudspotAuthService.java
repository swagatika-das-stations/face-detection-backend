package com.stations.facedetection.integration.kloudspot.service;

import java.util.Base64;
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
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private long tokenExpiryTime;

    public synchronized String getToken() {

        long currentTime = System.currentTimeMillis();

        // reuse token if not expired
        if (cachedToken != null && currentTime < tokenExpiryTime - 60000) {
            log.info("Using cached Kloudspot JWT token");
            return cachedToken;
        }

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

            String token = extractToken(response);

            cachedToken = token;

            // extract expiry from JWT
            tokenExpiryTime = extractExpiryFromToken(token);

            log.info("Kloudspot JWT token generated successfully (length={})", token.length());

            return cachedToken;

        } catch (Exception ex) {
            log.error("Failed to generate JWT token from Kloudspot API", ex);
            throw new RuntimeException("Kloudspot authentication failed", ex);
        }
    }

    private String extractToken(String response) {

        try {
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("token")) {
                return jsonNode.get("token").asText().trim();
            }

        } catch (Exception ignored) {}

        return response.replace("\"", "").trim();
    }

    private long extractExpiryFromToken(String token) {

        try {

            String[] parts = token.split("\\.");

            String payload = new String(
                    Base64.getUrlDecoder().decode(parts[1])
            );

            JsonNode jsonNode = objectMapper.readTree(payload);

            if (jsonNode.has("exp")) {

                long expSeconds = jsonNode.get("exp").asLong();

                return expSeconds * 1000; // convert to milliseconds
            }

        } catch (Exception e) {
            log.warn("Could not extract expiry from JWT", e);
        }

        // fallback (1 hour)
        return System.currentTimeMillis() + (60 * 60 * 1000);
    }
}