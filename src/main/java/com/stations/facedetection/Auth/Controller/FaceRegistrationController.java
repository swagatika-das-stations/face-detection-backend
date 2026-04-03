package com.stations.facedetection.Auth.Controller;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stations.facedetection.integration.kloudspot.DTO.RegistrationResponseDto;
import com.stations.facedetection.integration.kloudspot.service.KloudspotFaceRegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/faces")
@RequiredArgsConstructor
public class FaceRegistrationController {

    private final KloudspotFaceRegistrationService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam("faceImages") MultipartFile[] images,  // ← MUST BE ARRAY
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String employeeid) {


        try {
            List<File> files = Arrays.stream(images)
                .map(this::convertToFile)
                .toList();

            RegistrationResponseDto response = service.registerPerson(
                files, firstName, lastName, email, employeeid);

            files.forEach(File::delete);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }

    private File convertToFile(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));

            File convFile = File.createTempFile("upload_", extension);
            file.transferTo(convFile);
            return convFile;
        } catch (Exception e) {
            throw new RuntimeException("File conversion failed", e);
        }
    }
}