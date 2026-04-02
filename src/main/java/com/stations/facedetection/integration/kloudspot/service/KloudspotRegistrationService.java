package com.stations.facedetection.integration.kloudspot.service;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.stations.facedetection.integration.kloudspot.DTO.KloudspotRegistrationRequestDTO;
import com.stations.facedetection.integration.kloudspot.DTO.RegistrationResponseDto;
import com.stations.facedetection.integration.kloudspot.config.KloudspotProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KloudspotRegistrationService {

    private final WebClient webClient;
    private final KloudspotAuthService authService;
    private final KloudspotProperties properties;

    public RegistrationResponseDto register(KloudspotRegistrationRequestDTO request) {

        String token = authService.getToken();
        String url = properties.getBaseUrl() + properties.getRegistrationUrl();

        return  webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                    status -> status.isError(),
                    clientResponse -> clientResponse.bodyToMono(String.class).map(errorBody -> {
                        System.err.println("HTTP Status: " + clientResponse.statusCode());
                        System.err.println("Response Body: " + errorBody);
                        return new RuntimeException("Kloudspot API Error: " + errorBody);
                    })
                )
                .bodyToMono(RegistrationResponseDto.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }
}