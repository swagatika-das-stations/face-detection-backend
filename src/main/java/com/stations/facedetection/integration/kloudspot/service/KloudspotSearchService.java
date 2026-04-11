package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.stations.facedetection.integration.kloudspot.DTO.SearchResponseDto;
import com.stations.facedetection.integration.kloudspot.config.KloudspotProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    /**
     * Check if an identity already exists in Kloudspot database
     * @param identity - email or unique identifier
     * @return true if identity exists in system, false otherwise
     */
    public boolean checkIdentityExists(String identity) {
        try {
            String token = authService.getToken();
            String url = properties.getBaseUrl() + "/advanced/api/v1/humans/get?identity=" + identity;

            log.info("🔍 Checking if identity exists in Kloudspot: {}", identity);

            SearchResponseDto response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(SearchResponseDto.class)
                    .block();

            if (response != null) {
                log.info(" Identity EXISTS in Kloudspot:");
                log.info("   - Name: {} {}", response.getFirstName(), response.getLastName());
                log.info("   - Email: {}", response.getEmailAddress());
                log.info("   - Status: {}", response.getRegistrationStatus());
                log.info("   - User ID: {}", response.getUserId());
                return true;
            }

            log.info("Identity not found (null response)");
            return false;

        } catch (WebClientResponseException.NotFound e) {
            log.info("Identity NOT FOUND in Kloudspot (404): {}", identity);
            return false;
        } catch (WebClientResponseException.Forbidden e) {
            log.warn(" Access FORBIDDEN to search identity (403): {}", identity);
            return false;
        } catch (WebClientResponseException.Conflict e) {
            log.warn("Identity CONFLICT (409): {}", identity);
            return true; // Treat conflict as exists
        } catch (WebClientResponseException.BadRequest e) {
            log.warn("Bad request for identity search (400): {}", identity);
            return false;
        } catch (WebClientResponseException e) {
            log.warn("Search API returned error ({}): {}", e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error checking identity existence: {}", e.getMessage());
            return false;
        }
    }
}