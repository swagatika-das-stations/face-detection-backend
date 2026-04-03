package com.stations.facedetection.integration.kloudspot.service;

import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

        try {
            return webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchangeToMono(response -> {
                        System.out.println("Response Status Code: " + response.statusCode());
                        System.out.println("Response Headers: " + response.headers().asHttpHeaders());
                        
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(RegistrationResponseDto.class);
                        } else {
                            return response.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        System.err.println("Error Response Body: " + errorBody);
                                        return Mono.error(new RuntimeException("API Error: " + errorBody));
                                    });
                        }
                    })
                    .timeout(Duration.ofMinutes(2))
                    .doOnError(error -> System.err.println("Request failed: " + error.getMessage()))
                    .block();
        } catch (Exception e) {
            System.err.println("Registration API call failed: " + e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }
}