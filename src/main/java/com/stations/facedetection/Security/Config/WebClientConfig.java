package com.stations.facedetection.Security.Config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

          @Bean
        public WebClient webClient() {

    HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMinutes(5));

    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
}
}
