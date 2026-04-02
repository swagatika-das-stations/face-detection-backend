package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

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
            // 1️. Create ZIP
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
            human.setIdentity(email); // Confirm uniqueness requirement
            KloudspotRegistrationRequestDTO.Meta meta = new KloudspotRegistrationRequestDTO.Meta();
            meta.setEmployeeId(employeeId);
            human.setMeta(meta);
            human.setTags(List.of("Customer", "Employee")); // optionally make configurable
            request.setHuman(human);
            request.setZipFile(base64Zip);

            System.out.println("ZIP size (bytes): " + zipBytes.length);
            System.out.println("Registering Human: " + firstName + " " + lastName + ", Email: " + email);
              

         // ... inside your method, after building the request DTO

         try {
             ObjectMapper mapper = new ObjectMapper();
             mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // skip nulls
             String jsonRequest = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
             System.out.println("Request JSON to Kloudspot:");
             System.out.println(jsonRequest);
         } catch (Exception e) {
             System.err.println("Failed to serialize request to JSON: " + e.getMessage());
         }
            // 4️. Call Kloudspot API
            RegistrationResponseDto response = registrationService.register(request);

            // 5️. Save in DB if successful
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
            throw new RuntimeException("Failed to register person with Kloudspot", e);

        } finally {
            // 6️. Clean up temporary ZIP file
            if (zipFile != null && zipFile.exists()) {
                boolean deleted = zipFile.delete();
                if (!deleted) {
                    System.err.println("Failed to delete temporary ZIP file: " + zipFile.getAbsolutePath());
                }
            }
        }
    }
}