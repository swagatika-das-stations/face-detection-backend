package com.stations.facedetection.integration.kloudspot.service;

import org.springframework.stereotype.Service;
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

    /**
     * Check if an identity already exists in Kloudspot database
     */
    public boolean checkIdentityExists(String identity) {

        try {

            String token = authService.getToken();

            String url = properties.getBaseUrl()
                    + properties.getSearchUrl()
                    + "?identity=" + identity;

            log.info("Checking Kloudspot identity existence for identity={}", identity);

            SearchResponseDto response = webClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(SearchResponseDto.class)
                    .block();

            if (response != null) {

                log.info("Identity exists in Kloudspot: name={} {}, email={}, status={}, userId={}",
                        response.getFirstName(),
                        response.getLastName(),
                        response.getEmailAddress(),
                        response.getRegistrationStatus(),
                        response.getUserId());

                return true;
            }

            log.info("Identity not found in Kloudspot for identity={}", identity);

            return false;

        } catch (WebClientResponseException.NotFound e) {

            log.info("Identity not found in Kloudspot (404). identity={}", identity);
            return false;

        } catch (WebClientResponseException.Forbidden e) {

            log.warn("Kloudspot search access forbidden (403). identity={}", identity);
            return false;

        } catch (WebClientResponseException.Conflict e) {

            log.warn("Kloudspot identity conflict (409). Treating as existing. identity={}", identity);
            return true;

        } catch (WebClientResponseException.BadRequest e) {

            log.warn("Kloudspot search bad request (400). identity={}", identity);
            return false;

        } catch (WebClientResponseException e) {

            log.error("Kloudspot search API error. status={}, message={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            return false;

        } catch (Exception e) {

            log.error("Unexpected error while checking Kloudspot identity existence", e);
            return false;
        }
    }
}