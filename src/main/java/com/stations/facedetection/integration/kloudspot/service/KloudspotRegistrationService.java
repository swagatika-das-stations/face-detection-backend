package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

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

    /**
     * Register person with Kloudspot using multipart/form-data
     * Sends human.json and image.zip as separate files
     */
    public RegistrationResponseDto register(File humanJsonFile, File imageZipFile) {

        String token = authService.getToken();
        String url = properties.getBaseUrl() + properties.getRegistrationUrl();

        log.info("Calling Kloudspot registration API: {}", url);
        log.info("Outgoing Kloudspot request parts: zipFile={}, human={}, imageZipSizeKB={}",
                imageZipFile.getName(), humanJsonFile.getName(), imageZipFile.length() / 1024);

        try {
            String humanJson = readFileContent(humanJsonFile);
            log.debug("Outgoing Kloudspot human.json payload: {}", humanJson);

            // Create multipart form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("zipFile", new FileSystemResource(imageZipFile));
            body.add("human", new FileSystemResource(humanJsonFile));

            return webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(body)
                    .exchangeToMono(response -> {

                        log.info("Kloudspot API Response Status: {}", response.statusCode());

                        if (response.statusCode().is2xxSuccessful()) {

                            return response.bodyToMono(RegistrationResponseDto.class)
                                    .doOnNext(body2 -> {
                                        body2.setStatus("successful");
                                        log.info("Kloudspot registration success. EntityId={}, Status=successful", body2.getEntityId());
                                    });

                        } else {

                            return response.bodyToMono(String.class)
                                    .flatMap(errorBody -> {

                                        log.error("Kloudspot API error response: {}", errorBody);

                                        return reactor.core.publisher.Mono.error(new RuntimeException(
                                                "Kloudspot API error: " + errorBody
                                        ));
                                    });
                        }
                    })

                    .timeout(Duration.ofMinutes(5))

                    .doOnError(error ->
                            log.error("Kloudspot registration request failed: {}", error.getMessage(), error)
                    )

                    .block();

        } catch (Exception e) {

            log.error("Kloudspot registration API call failed", e);

            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    private String readFileContent(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (Exception e) {
            log.warn("Failed to read file content for logging: {}", file.getName(), e);
            return "<unable to read file>";
        }
    }
}