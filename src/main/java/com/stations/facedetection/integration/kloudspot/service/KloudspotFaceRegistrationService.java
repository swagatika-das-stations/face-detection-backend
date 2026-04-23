package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.stations.facedetection.User.Entity.EmployeeEntity;
import com.stations.facedetection.User.Entity.FaceImageEntity;
import com.stations.facedetection.User.Entity.FaceRegistryEntity;
import com.stations.facedetection.User.Entity.RoleEntity;
import com.stations.facedetection.User.Entity.UserEntity;
import com.stations.facedetection.User.Entity.UserRoleEntity;
import com.stations.facedetection.User.Repository.EmployeeRepository;
import com.stations.facedetection.User.Repository.FaceImageRepository;
import com.stations.facedetection.User.Repository.FaceRegistryRepository;
import com.stations.facedetection.User.Repository.RoleRepository;
import com.stations.facedetection.User.Repository.UserRepository;
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
    private final EmployeeRepository employeeRepository;
    private final FaceImageRepository faceImageRepository;
    private final FaceRegistryRepository faceRegistryRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final KloudspotUploadConfig uploadConfig;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RegistrationResponseDto registerPerson(List<File> images,
                                                   String firstName,
                                                   String lastName,
                                                   String email,
                                                   String employeeid,
                                                   String password) {

        File imageZipFile = null;
        File humanJsonFile = null;
        List<File> cleanedImages = new ArrayList<>();
        FaceRegistryEntity attemptHistory = buildAttemptHistory(firstName, lastName, email, employeeid);

        try {

            // Persist attempt history later when all relation fields are available.
            // This avoids early FK failures from partially populated rows.

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
                existsResponse.setStatus("successful");
                existsResponse.setMessage("Identity " + email + " already exists in Kloudspot; local database synced successfully");

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

            if (response == null) {
                RegistrationResponseDto emptyResponse = new RegistrationResponseDto();
                emptyResponse.setStatus("failure");
                emptyResponse.setMessage("Kloudspot registration failed with empty response");
                tryMarkHistoryAsFailure(attemptHistory, emptyResponse.getMessage());
                return emptyResponse;
            }

            // 8️ Handle response
            if (response != null && "successful".equalsIgnoreCase(response.getStatus())) {

                log.info("Kloudspot registration successful. EntityId={}", response.getEntityId());
                
                // Only save if entityId is present
                if (response.getEntityId() != null && !response.getEntityId().trim().isEmpty()) {
                    saveSuccessfulRegistration(firstName, lastName, email, employeeid, password, images, response.getEntityId(), attemptHistory);
                } else {
                    log.warn("Kloudspot registration returned success but no entityId. Skipping database save.");
                    tryMarkHistoryAsFailure(attemptHistory, "Registration successful but no entityId returned from Kloudspot");
                    response.setStatus("failure");
                    response.setMessage("Registration failed: No entity ID returned from Kloudspot");
                }

            } else if (response != null && "ALREADY_EXISTS".equalsIgnoreCase(response.getStatus())) {

                log.warn("Person already exists in Kloudspot");
                saveSuccessfulRegistration(firstName, lastName, email, employeeid, password, images, null, attemptHistory);
                response.setStatus("successful");
                response.setMessage("Identity already exists in Kloudspot; local database synced successfully");

            } else {

                log.error("Kloudspot registration failed. Response={}", response);
                if (response != null) {
                    response.setStatus("failure");
                    tryMarkHistoryAsFailure(attemptHistory, response.getMessage());
                } else {
                    tryMarkHistoryAsFailure(attemptHistory, "Kloudspot registration failed with empty response");
                }
            }

            return response;

        } catch (IllegalArgumentException e) {

            log.error("Image validation failed: {}", e.getMessage());
            tryMarkHistoryAsFailure(attemptHistory, e.getMessage());
            RegistrationResponseDto errorResponse = new RegistrationResponseDto();
            errorResponse.setStatus("failure");
            errorResponse.setMessage(e.getMessage());
            return errorResponse;

        } catch (Exception e) {

            log.error("Face registration process failed", e);
            tryMarkHistoryAsFailure(attemptHistory, e.getMessage());
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
    private void saveSuccessfulRegistration(String firstName,
                                            String lastName,
                                            String email,
                                            String employeeid,
                                            String password,
                                            List<File> images,
                                            String entityId,
                                            FaceRegistryEntity attemptHistory) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUserWithRole(email, password));

        EmployeeEntity employee = resolveEmployeeForUser(user, firstName, lastName, employeeid);
        if (StringUtils.hasText(entityId)) {
            employee.setEntityId(entityId);
        }
        employee = employeeRepository.save(employee);

        try {
            List<FaceImageEntity> faceImages = copyAndBuildFaceImages(images, employeeid, employee);
            faceImageRepository.saveAll(faceImages);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist face images", e);
        }

        attemptHistory.setUser(user);
        attemptHistory.setEmployee(employee);
        attemptHistory.setEntityId(entityId);
        attemptHistory.setRegistrationStatus("SUCCESS");
        attemptHistory.setFailureReason(null);
        faceRegistryRepository.save(attemptHistory);

        log.info("Registration success persisted for email={}, entityId={}, employeeRegisterId={}",
            email, entityId, employee.getId());
    }

    private UserEntity createUserWithRole(String email, String password) {

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);

        RoleEntity employeeRole = roleRepository.findByName("EMPLOYEE")
                .orElseGet(() -> {
                    RoleEntity role = new RoleEntity();
                    role.setName("EMPLOYEE");
                    return roleRepository.save(role);
                });

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUsers(user);
        userRole.setRoles(employeeRole);

        List<UserRoleEntity> roles = new ArrayList<>();
        roles.add(userRole);
        user.setUserRoles(roles);

        return userRepository.save(user);
    }

    private EmployeeEntity resolveEmployeeForUser(UserEntity user,
                                                  String firstName,
                                                  String lastName,
                                                  String employeeid) {

        Optional<EmployeeEntity> existingByUser = employeeRepository.findByUserId(user.getId());
        EmployeeEntity employee = existingByUser
                .orElseGet(() -> employeeRepository.findByEmployeeId(employeeid).orElse(new EmployeeEntity()));

        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmployeeId(employeeid);
        employee.setUser(user);

        EmployeeEntity savedEmployee = employeeRepository.save(employee);
        user.setEmployee(savedEmployee);

        return savedEmployee;
    }

    private FaceRegistryEntity buildAttemptHistory(String firstName,
                                                   String lastName,
                                                   String email,
                                                   String employeeid) {

        FaceRegistryEntity history = new FaceRegistryEntity();
        history.setFirstName(firstName);
        history.setLastName(lastName);
        history.setEmail(email);
        history.setEmployeeId(employeeid);
        history.setRegistrationStatus("FAILED");
        history.setAttemptedAt(LocalDateTime.now());

        userRepository.findByEmail(email).ifPresent(history::setUser);

        return history;
    }

    private void markHistoryAsFailure(FaceRegistryEntity history, String message) {

        if (history == null) {
            return;
        }

        history.setRegistrationStatus("FAILED");
        history.setFailureReason(message);
        faceRegistryRepository.save(history);
    }

    private void tryMarkHistoryAsFailure(FaceRegistryEntity history, String message) {

        try {
            markHistoryAsFailure(history, message);
        } catch (Exception ex) {
            log.warn("Failed to persist failure history. OriginalMessage={}, SaveError={}",
                    message,
                    ex.getMessage());
        }
    }

    private List<FaceImageEntity> copyAndBuildFaceImages(List<File> images,
                                                          String employeeid,
                                                          EmployeeEntity employee) throws IOException {

        List<FaceImageEntity> faceImageEntities = new ArrayList<>();

        for (File sourceFile : images) {
            byte[] imageBytes = Files.readAllBytes(sourceFile.toPath());

            FaceImageEntity faceImage = new FaceImageEntity();
            faceImage.setEmployee(employee);
            faceImage.setImageData(imageBytes);

            faceImageEntities.add(faceImage);
        }

        return faceImageEntities;
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