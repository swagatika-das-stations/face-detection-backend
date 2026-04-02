package com.stations.facedetection.integration.kloudspot.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.stations.facedetection.integration.kloudspot.config.KloudspotProperties;
import com.stations.facedetection.integration.kloudspot.service.KloudspotAuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KloudspotClient {

    private final WebClient webClient;
    private final KloudspotAuthService authService;
    private final KloudspotProperties properties;

    public WebClient.RequestHeadersSpec<?> authorizedPost(String url, Object body) {

        String token = authService.getToken();

        return webClient.post()
                .uri(properties.getBaseUrl() + url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.ALL)
                .bodyValue(body);
    }
} 