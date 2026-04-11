package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
import com.stations.facedetection.integration.kloudspot.config.KloudspotUploadConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KloudspotFaceRegistrationService {

    private final ImageValidator imageValidator;
    private final ImageCleaner imageCleaner;
    private final ZipBuilder zipBuilder;
    private final KloudspotRegistrationService registrationService;
    private final KloudspotSearchService searchService;
    private final FaceRegistryRepository repository;
    private final KloudspotUploadConfig uploadConfig;

    public RegistrationResponseDto registerPerson(List<File> images,
                                                  String firstName,
                                                  String lastName,
                                                  String email,
                                                  String employeeid) {

        File zipFile = null;
        List<File> cleanedImages = new ArrayList<>();

        try {
            log.info("Starting registration for {} {} (Employee ID: {})", firstName, lastName, employeeid);
            log.info("Kloudspot Specifications:");
            log.info("   - Images: {}-{}", uploadConfig.getMinImages(), uploadConfig.getMaxImages());
            log.info("   - Max Image Size: {} MB", uploadConfig.getMaxImageSizeMb());
            log.info("   - Supported Formats: {}", String.join(", ", uploadConfig.getSupportedFormats()));
            //  Check if identity already exists
            log.info(" Checking if identity already exists in Kloudspot database...");
            if (searchService.checkIdentityExists(email)) {
                log.warn(" Identity {} already exists in Kloudspot database", email);
                RegistrationResponseDto existsResponse = new RegistrationResponseDto();
                existsResponse.setSTATUS("ALREADY_EXISTS");
                existsResponse.setMessage("Identity " + email + " already exists in Kloudspot database");
                return existsResponse;
            }
            log.info(" Identity does not exist, proceeding with registration");
            // 1️. Validate images according to Kloudspot specs
            imageValidator.validateImages(images);

            // 2️. Clean and optimize each image
            log.info("Cleaning {} images...", images.size());
            for (int i = 0; i < images.size(); i++) {
                log.info("--- Processing Image {} of {} ---", i + 1, images.size());
                File cleaned = imageCleaner.cleanImage(images.get(i));
                cleanedImages.add(cleaned);
            }

            // 3️. Create ZIP
            zipFile = zipBuilder.createZip(cleanedImages);
            byte[] zipBytes = Files.readAllBytes(zipFile.toPath());
            long zipSize = zipBytes.length;
            
            log.info("ZIP File:");
            log.info("   - Size: {} KB ({} MB)", 
                    zipSize / 1024, 
                    String.format("%.2f", zipSize / (1024.0 * 1024.0)));
            log.info("   - Images: {}", cleanedImages.size());

            // 4️. Convert to Base64
            String base64Zip = Base64.getEncoder().encodeToString(zipBytes);
            log.info("Base64 encoded: {} characters", base64Zip.length());

            // 5️. Build Request
            KloudspotRegistrationRequestDTO request = buildRequest(
                    firstName, lastName, email, employeeid, base64Zip);

            // 6️. Print JSON to console (truncate Base64) - FOR DEBUGGING
            logRequestJson(request, base64Zip, zipBytes, firstName, lastName, email);

            // 7️. Send to Kloudspot
            log.info("Sending registration request to Kloudspot...");
            RegistrationResponseDto response = registrationService.register(request);

            // 8️. Save to database if successful
            if (response != null && "SUCCESS".equalsIgnoreCase(response.getSTATUS())) {
                saveToDatabase(firstName, lastName, email, employeeid, response);
                log.info(" Registration successful! Entity ID: {}", response.getEntityId());
            } else {
                log.error(" Kloudspot registration failed: {}", response);
            }
            System.out.println(response);
            return response;

        } catch (IllegalArgumentException e) {
            log.error(" Validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(" Registration failed", e);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);

        } finally {
            // Cleanup temporary files
            cleanupTempFile(zipFile);
            cleanedImages.forEach(this::cleanupTempFile);
        }
    }

    private KloudspotRegistrationRequestDTO buildRequest(
            String firstName, String lastName, String email, String employeeid, String base64Zip) {
        
        KloudspotRegistrationRequestDTO request = new KloudspotRegistrationRequestDTO();
        KloudspotRegistrationRequestDTO.Human human = new KloudspotRegistrationRequestDTO.Human();
        
        human.setFirstName(firstName);
        human.setLastName(lastName);
        human.setEmailId(email);
        human.setIdentity(email);

        KloudspotRegistrationRequestDTO.Meta meta = new KloudspotRegistrationRequestDTO.Meta();
        meta.setEmployeeid(employeeid);
        human.setMeta(meta);
        
        human.setTags(List.of("Customer", "Employee"));
        
        request.setHuman(human);
        request.setZipFile(base64Zip);
        
        return request;
    }

    private void logRequestJson(KloudspotRegistrationRequestDTO request, 
                                 String base64Zip, 
                                 byte[] zipBytes,
                                 String firstName, 
                                 String lastName, 
                                 String email) {
        try {
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
            
        } catch (Exception e) {
            log.warn("Failed to log request JSON", e);
        }
    }

    private void saveToDatabase(String firstName, String lastName, String email, 
                                String employeeid, RegistrationResponseDto response) {
        FaceRegistryEntity user = new FaceRegistryEntity();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEmployeeId(employeeid);

        user.setEntityId(response.getEntityId());

        repository.save(user);
        log.info(" Saved to database");
    }

    private void cleanupTempFile(File file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
                log.debug(" Deleted temp file: {}", file.getName());
            } catch (Exception e) {
                log.warn("Failed to delete temp file: {}", file.getName());
            }
        }
    }
}