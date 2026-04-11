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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KloudspotRegistrationService {

    private final WebClient webClient;
    private final KloudspotAuthService authService;
    private final KloudspotProperties properties;

    public RegistrationResponseDto register(KloudspotRegistrationRequestDTO request) {

        String token = authService.getToken();
        String url = properties.getBaseUrl() + properties.getRegistrationUrl();

        log.info("Calling Kloudspot registration API: {}", url);

        try {

            return webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)

                    .exchangeToMono(response -> {

                        log.info("Kloudspot API Response Status: {}", response.statusCode());

                        if (response.statusCode().is2xxSuccessful()) {

                            return response.bodyToMono(RegistrationResponseDto.class)
                                    .doOnNext(body ->
                                            log.info("Kloudspot registration success. EntityId={}", body.getEntityId())
                                    );

                        } else {

                            return response.bodyToMono(String.class)
                                    .flatMap(errorBody -> {

                                        log.error("Kloudspot API error response: {}", errorBody);

                                        return Mono.error(new RuntimeException(
                                                "Kloudspot API error: " + errorBody
                                        ));
                                    });
                        }
                    })

                    .timeout(Duration.ofMinutes(2))

                    .doOnError(error ->
                            log.error("Kloudspot registration request failed: {}", error.getMessage(), error)
                    )

                    .block();

        } catch (Exception e) {

            log.error("Kloudspot registration API call failed", e);

            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }
}