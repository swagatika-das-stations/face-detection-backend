package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stations.facedetection.User.Entity.FaceRegistryEntity;
import com.stations.facedetection.User.Repository.FaceRegistryRepository;
import com.stations.facedetection.integration.kloudspot.DTO.KloudspotRegistrationRequestDTO;
import com.stations.facedetection.integration.kloudspot.DTO.RegistrationResponseDto;
import com.stations.facedetection.integration.kloudspot.builder.ZipBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KloudspotFaceRegistrationService {

    private final ZipBuilder zipBuilder;
    private final KloudspotRegistrationService registrationService;
    private final FaceRegistryRepository repository;

    public RegistrationResponseDto registerPerson(List<File> images,
                                                  String firstName,
                                                  String lastName,
                                                  String email,
                                                  String employeeId) {

        File zipFile = null;

        try {
            // 1️. Create ZIP from images
            zipFile = zipBuilder.createZip(images);

            // 2️. Convert ZIP → Base64
            byte[] zipBytes = Files.readAllBytes(zipFile.toPath());
            String base64Zip = Base64.getEncoder().encodeToString(zipBytes);

            // 3️. Build Request DTO
            KloudspotRegistrationRequestDTO request = new KloudspotRegistrationRequestDTO();
            KloudspotRegistrationRequestDTO.Human human = new KloudspotRegistrationRequestDTO.Human();
            human.setFirstName(firstName);
            human.setLastName(lastName);
            human.setEmailId(email);
            human.setIdentity(email);

            KloudspotRegistrationRequestDTO.Meta meta = new KloudspotRegistrationRequestDTO.Meta();
            meta.setEmployeeid(employeeId);
            human.setMeta(meta);
            human.setTags(List.of("Customer", "Employee"));
            request.setHuman(human);
            request.setZipFile(base64Zip);

            // 4️. Print JSON to console (truncate Base64)
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String jsonRequest = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

         // Truncate Base64 in logs
            int maxLogLength = 100;
            Pattern pattern = Pattern.compile("(\"zipFile\"\\s*:\\s*\")(.*?)(\")");
            Matcher matcher = pattern.matcher(jsonRequest);
            String safeJsonRequest;
            if (matcher.find()) {
                String truncatedBase64 = base64Zip.substring(0, Math.min(maxLogLength, base64Zip.length())) + "...";
                safeJsonRequest = matcher.replaceAll("$1" + truncatedBase64 + "$3");
            } else {
                safeJsonRequest = jsonRequest;
            }
            System.out.println("Request JSON to Kloudspot (Base64 truncated):");
            System.out.println(safeJsonRequest);

            System.out.println("ZIP size (bytes): " + zipBytes.length);
            System.out.println("Registering Human: " + firstName + " " + lastName + ", Email: " + email);

            // 5️. Call Kloudspot API
            RegistrationResponseDto response = registrationService.register(request);

            // 6️. Save in DB if registration successful
            if (response != null && "SUCCESS".equalsIgnoreCase(response.getSTATUS())) {
                FaceRegistryEntity user = new FaceRegistryEntity();
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                user.setEmployeeId(employeeId);
                user.setEntityId(response.getEntityId());
                repository.save(user);
            } else {
                System.err.println("Kloudspot registration failed or returned null: " + response);
            }

            System.out.println("Kloudspot Response STATUS: " + (response != null ? response.getSTATUS() : "null"));
            return response;

        } catch (Exception e) {
            System.err.println("Failed to register person with Kloudspot");
            e.printStackTrace();
            throw new RuntimeException("Registration failed", e);

        } finally {
            // 7️. Clean up temporary ZIP file safely
            if (zipFile != null) {
                try {
                    Files.deleteIfExists(zipFile.toPath());
                } catch (Exception e) {
                    System.err.println("Failed to delete temporary ZIP file: " + zipFile.getAbsolutePath());
                    e.printStackTrace();
                }
            }
        }
    }
}