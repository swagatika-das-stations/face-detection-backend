package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stations.facedetection.User.Entity.FaceRegistryEntity;
import com.stations.facedetection.User.Entity.RoleEntity;
import com.stations.facedetection.User.Entity.UserEntity;
import com.stations.facedetection.User.Entity.UserRoleEntity;
import com.stations.facedetection.User.Repository.FaceRegistryRepository;
import com.stations.facedetection.User.Repository.RoleRepository;
import com.stations.facedetection.User.Repository.UserRepository;
import com.stations.facedetection.User.Repository.UserRoleRepository;
import com.stations.facedetection.integration.kloudspot.DTO.KloudspotRegistrationRequestDTO;
import com.stations.facedetection.integration.kloudspot.DTO.RegistrationResponseDto;
import com.stations.facedetection.integration.kloudspot.builder.ZipBuilder;
import com.stations.facedetection.integration.kloudspot.config.KloudspotUploadConfig;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KloudspotFaceRegistrationService {

    private final ImageValidator imageValidator;
    private final ImageCleaner imageCleaner;
    private final ZipBuilder zipBuilder;
    private final KloudspotRegistrationService registrationService;
    private final KloudspotSearchService searchService;
    private final FaceRegistryRepository repository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final KloudspotUploadConfig uploadConfig;

    public RegistrationResponseDto registerPerson(List<File> images,
                                                   String firstName,
                                                   String lastName,
                                                   String email,
                                                   String employeeid,
                                                   String password) {

        File imageZipFile = null;
        File humanJsonFile = null;
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
                existsResponse.setStatus("failure");
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

            // 3️ Build human data
            KloudspotRegistrationRequestDTO humanData =
                    buildRequest(firstName, lastName, email, employeeid);

            // 4️ Create human.json file
            humanJsonFile = zipBuilder.createHumanJson(humanData);

            // 5️ Create image ZIP
            imageZipFile = zipBuilder.createImageZip(cleanedImages);

            long zipSize = imageZipFile.length();

            log.info("Image ZIP created successfully. Size={} KB ({} MB), images={}",
                    zipSize / 1024,
                    String.format("%.2f", zipSize / (1024.0 * 1024.0)),
                    cleanedImages.size());
            
            // Validate ZIP size
            long maxZipSize = uploadConfig.getMaxTotalZipSizeBytes();
            if (zipSize > maxZipSize) {
                throw new RuntimeException(
                    String.format("Image ZIP file exceeds max size. Current: %d KB, Max: %d KB",
                        zipSize / 1024, maxZipSize / 1024)
                );
            }

            // 6️ Send request
            log.info("Sending face registration request to Kloudspot (human.json + image.zip)");

            RegistrationResponseDto response = registrationService.register(humanJsonFile, imageZipFile);

            // 8️ Handle response
            if (response != null && "successful".equalsIgnoreCase(response.getStatus())) {

                log.info("Kloudspot registration successful. EntityId={}", response.getEntityId());

                saveToDatabase(firstName, lastName, email, employeeid, password, response);

            } else if (response != null && "ALREADY_EXISTS".equalsIgnoreCase(response.getStatus())) {

                log.warn("Person already exists in Kloudspot");

            } else {

                log.error("Kloudspot registration failed. Response={}", response);
                if (response != null) {
                    response.setStatus("failure");
                }
            }

            return response;

        } catch (IllegalArgumentException e) {

            log.error("Image validation failed: {}", e.getMessage());
            RegistrationResponseDto errorResponse = new RegistrationResponseDto();
            errorResponse.setStatus("failure");
            errorResponse.setMessage(e.getMessage());
            return errorResponse;

        } catch (Exception e) {

            log.error("Face registration process failed", e);
            RegistrationResponseDto errorResponse = new RegistrationResponseDto();
            errorResponse.setStatus("failure");
            errorResponse.setMessage("Registration failed: " + e.getMessage());
            return errorResponse;

        } finally {

            log.debug("Cleaning temporary files");

            cleanupTempFile(imageZipFile);
            cleanupTempFile(humanJsonFile);
            cleanedImages.forEach(this::cleanupTempFile);
        }
    }

    private KloudspotRegistrationRequestDTO buildRequest(
            String firstName,
            String lastName,
            String email,
            String employeeid) {

        KloudspotRegistrationRequestDTO request = new KloudspotRegistrationRequestDTO();

        request.setIdentity(email);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmailAddress(email);

        KloudspotRegistrationRequestDTO.Meta meta = new KloudspotRegistrationRequestDTO.Meta();
        meta.setEmployeeId(employeeid);
        request.setMeta(meta);

        log.info("Built human data: identity={}, firstName={}, lastName={}, employeeId={}",
                email, firstName, lastName, employeeid);

        return request;
    }
@Transactional
private void saveToDatabase(String firstName,
                            String lastName,
                            String email,
                            String employeeid,
                            String password,
                            RegistrationResponseDto response) {

    // Check if user already exists
    if (userRepository.findByEmail(email).isPresent()) {
        log.warn("User already exists with email={}. Skipping user creation.", email);
        return;
    }

    // Create User
    UserEntity user = new UserEntity();
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setEnabled(true);

    // Create Face Registry
    FaceRegistryEntity faceRegistry = new FaceRegistryEntity();
    faceRegistry.setFirstName(firstName);
    faceRegistry.setLastName(lastName);
    faceRegistry.setEmail(email);
    faceRegistry.setEmployeeId(employeeid);
    faceRegistry.setEntityId(response.getEntityId());

    // Set One-to-One relationship
    faceRegistry.setUser(user);
    user.setFaceRegistry(faceRegistry);

    // Get or create role
    RoleEntity employeeRole = roleRepository.findByName("EMPLOYEE")
            .orElseGet(() -> {
                RoleEntity role = new RoleEntity();
                role.setName("EMPLOYEE");
                return roleRepository.save(role);
            });

    // Create UserRole mapping
    UserRoleEntity userRole = new UserRoleEntity();
    userRole.setUsers(user);
    userRole.setRoles(employeeRole);

    List<UserRoleEntity> roles = new ArrayList<>();
    roles.add(userRole);
    user.setUserRoles(roles);

    // Save user (cascade will save face_registry)
    userRepository.save(user);

    log.info("User saved successfully with email={}", email);
    log.info("Face registry saved with entityId={}", response.getEntityId());
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