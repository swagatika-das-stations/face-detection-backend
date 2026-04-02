package com.stations.facedetection.Auth.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stations.facedetection.integration.kloudspot.config.KloudspotProperties;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final KloudspotProperties properties;

    @GetMapping("/test")
    public String test() {
        System.out.println("Swagatika");
        return "successfull";
    }
}