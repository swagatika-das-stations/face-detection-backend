package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.stations.facedetection.integration.kloudspot.config.KloudspotProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KloudspotSearchService {

    private final WebClient webClient;
    private final KloudspotAuthService authService;
    private final KloudspotProperties properties;

    public String register(File zipFile, File jsonFile) {

        String token = authService.getToken();

        String url = properties.getBaseUrl() + properties.getRegistrationUrl();

        // Prepare multipart data
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("zipFile", new FileSystemResource(zipFile));
        body.add("human", new FileSystemResource(jsonFile));

        String response = webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }
}