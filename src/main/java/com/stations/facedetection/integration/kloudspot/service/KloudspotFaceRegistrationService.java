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

            log.info("Starting Kloudspot face registration for employeeId={}, email={}", employeeid, email);

            log.info("Upload configuration: minImages={}, maxImages={}, maxImageSizeMB={}, formats={}",
                    uploadConfig.getMinImages(),
                    uploadConfig.getMaxImages(),
                    uploadConfig.getMaxImageSizeMb(),
                    uploadConfig.getSupportedFormats());

            // Check if identity exists
            log.info("Checking if identity exists in Kloudspot for email={}", email);

            if (searchService.checkIdentityExists(email)) {

                log.warn("Identity already exists in Kloudspot database for email={}", email);

                RegistrationResponseDto existsResponse = new RegistrationResponseDto();
                existsResponse.setSTATUS("ALREADY_EXISTS");
                existsResponse.setMessage("Identity " + email + " already exists in Kloudspot database");

                return existsResponse;
            }

            log.info("Identity not found. Proceeding with registration.");

            // 1️ Validate images
            log.info("Validating {} uploaded images", images.size());
            imageValidator.validateImages(images);

            // 2️ Clean images
            log.info("Cleaning uploaded images");

            for (int i = 0; i < images.size(); i++) {

                log.debug("Cleaning image {} of {}", i + 1, images.size());

                File cleaned = imageCleaner.cleanImage(images.get(i));
                cleanedImages.add(cleaned);
            }

            log.info("Image cleaning completed. Cleaned images={}", cleanedImages.size());

            // 3️ Create ZIP
            zipFile = zipBuilder.createZip(cleanedImages);

            byte[] zipBytes = Files.readAllBytes(zipFile.toPath());

            long zipSize = zipBytes.length;

            log.info("ZIP created successfully. Size={} KB ({} MB), images={}",
                    zipSize / 1024,
                    String.format("%.2f", zipSize / (1024.0 * 1024.0)),
                    cleanedImages.size());

            // 4️ Convert to Base64
            String base64Zip = Base64.getEncoder().encodeToString(zipBytes);

            log.debug("Base64 ZIP length={}", base64Zip.length());

            // 5️ Build request
            KloudspotRegistrationRequestDTO request =
                    buildRequest(firstName, lastName, email, employeeid, base64Zip);

            // 6️ Log request JSON (safe)
            logRequestJson(request, base64Zip);

            // 7️ Send request
            log.info("Sending face registration request to Kloudspot");

            RegistrationResponseDto response = registrationService.register(request);

            // 8️ Handle response
            if (response != null && "SUCCESS".equalsIgnoreCase(response.getSTATUS())) {

                log.info("Kloudspot registration successful. EntityId={}", response.getEntityId());

                saveToDatabase(firstName, lastName, email, employeeid, response);

            } else {

                log.error("Kloudspot registration failed. Response={}", response);
            }

            return response;

        } catch (IllegalArgumentException e) {

            log.error("Image validation failed: {}", e.getMessage());
            throw e;

        } catch (Exception e) {

            log.error("Face registration process failed", e);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);

        } finally {

            log.debug("Cleaning temporary files");

            cleanupTempFile(zipFile);
            cleanedImages.forEach(this::cleanupTempFile);
        }
    }

    private KloudspotRegistrationRequestDTO buildRequest(
            String firstName,
            String lastName,
            String email,
            String employeeid,
            String base64Zip) {

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

    private void logRequestJson(KloudspotRegistrationRequestDTO request, String base64Zip) {

        try {

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            String jsonRequest = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);

            Pattern pattern = Pattern.compile("(\"zipFile\"\\s*:\\s*\")(.*?)(\")");

            Matcher matcher = pattern.matcher(jsonRequest);

            String safeJson;

            if (matcher.find()) {

                String truncated = base64Zip.substring(0, Math.min(100, base64Zip.length())) + "...";

                safeJson = matcher.replaceAll("$1" + truncated + "$3");

            } else {

                safeJson = jsonRequest;
            }

            log.debug("Kloudspot request JSON (Base64 truncated): {}", safeJson);

        } catch (Exception e) {

            log.warn("Failed to log request JSON", e);
        }
    }

    private void saveToDatabase(String firstName,
                                String lastName,
                                String email,
                                String employeeid,
                                RegistrationResponseDto response) {

        FaceRegistryEntity user = new FaceRegistryEntity();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEmployeeId(employeeid);
        user.setEntityId(response.getEntityId());

        repository.save(user);

        log.info("Face registry record saved to database. entityId={}", response.getEntityId());
    }

    private void cleanupTempFile(File file) {

        if (file != null) {

            try {

                Files.deleteIfExists(file.toPath());

                log.debug("Temporary file deleted: {}", file.getName());

            } catch (Exception e) {

                log.warn("Failed to delete temporary file: {}", file.getName());
            }
        }
    }
}