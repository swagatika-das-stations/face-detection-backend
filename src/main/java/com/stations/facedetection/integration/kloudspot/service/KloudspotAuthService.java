package com.stations.facedetection.integration.kloudspot.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.stations.facedetection.integration.kloudspot.config.KloudspotProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KloudspotAuthService {

    private final WebClient webClient;
    private final KloudspotProperties properties;

    public String getToken() {

        String url = properties.getBaseUrl() + properties.getAuthUrl();
        
        System.out.println(properties);
        
        Map<String,String> body = new HashMap<>();
        body.put("id", properties.getId());
        body.put("secretKey", properties.getSecretKey());

        String token = webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        System.out.println("JWT Token: " + token);
        
        return token;
    }
}