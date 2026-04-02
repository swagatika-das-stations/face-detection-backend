package com.stations.facedetection.integration.kloudspot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "kloudspot")
public class KloudspotProperties {

    private String baseUrl;
    private String authUrl;
    private String registrationUrl;
    private String searchUrl;

    private String id;
    private String secretKey;
}