package com.stations.facedetection.integration.kloudspot.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
    private final EmployeeRepository employeeRepository;
    private final FaceImageRepository faceImageRepository;
    private final FaceRegistryRepository faceRegistryRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final KloudspotUploadConfig uploadConfig;

    @Value("${app.media.root-dir:media}")
    private String mediaRootDir;

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

            attemptHistory = faceRegistryRepository.save(attemptHistory);

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

                markHistoryAsFailure(attemptHistory, existsResponse.getMessage());

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

                if (StringUtils.hasText(response.getEntityId())) {
                    saveSuccessfulRegistration(firstName, lastName, email, employeeid, password, images, response, attemptHistory);
                } else {
                    markHistoryAsFailure(attemptHistory, "Successful status but missing entityId from Kloudspot response");
                }

            } else if (response != null && "ALREADY_EXISTS".equalsIgnoreCase(response.getStatus())) {

                log.warn("Person already exists in Kloudspot");
                markHistoryAsFailure(attemptHistory, response.getMessage());

            } else {

                log.error("Kloudspot registration failed. Response={}", response);
                if (response != null) {
                    response.setStatus("failure");
                    markHistoryAsFailure(attemptHistory, response.getMessage());
                } else {
                    markHistoryAsFailure(attemptHistory, "Kloudspot registration failed with empty response");
                }
            }

            return response;

        } catch (IllegalArgumentException e) {

            log.error("Image validation failed: {}", e.getMessage());
            markHistoryAsFailure(attemptHistory, e.getMessage());
            RegistrationResponseDto errorResponse = new RegistrationResponseDto();
            errorResponse.setStatus("failure");
            errorResponse.setMessage(e.getMessage());
            return errorResponse;

        } catch (Exception e) {

            log.error("Face registration process failed", e);
            markHistoryAsFailure(attemptHistory, e.getMessage());
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
    private void saveSuccessfulRegistration(String firstName,
                                            String lastName,
                                            String email,
                                            String employeeid,
                                            String password,
                                            List<File> images,
                                            RegistrationResponseDto response,
                                            FaceRegistryEntity attemptHistory) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUserWithRole(email, password));

        EmployeeEntity employee = resolveEmployeeForUser(user, firstName, lastName, employeeid);
        employee.setEntityId(response.getEntityId());
        employee = employeeRepository.save(employee);

        List<Path> copiedFilePaths = new ArrayList<>();

        try {
            List<FaceImageEntity> faceImages = copyAndBuildFaceImages(images, employeeid, employee, copiedFilePaths);
            faceImageRepository.saveAll(faceImages);
        } catch (Exception e) {
            cleanupCopiedFiles(copiedFilePaths);
            throw new RuntimeException("Failed to persist face images", e);
        }

        attemptHistory.setUser(user);
        attemptHistory.setEmployee(employee);
        attemptHistory.setEntityId(response.getEntityId());
        attemptHistory.setRegistrationStatus("SUCCESS");
        attemptHistory.setFailureReason(null);
        faceRegistryRepository.save(attemptHistory);

        log.info("Registration success persisted for email={}, entityId={}, employeeRegisterId={}",
                email, response.getEntityId(), employee.getId());
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

    private List<FaceImageEntity> copyAndBuildFaceImages(List<File> images,
                                                          String employeeid,
                                                          EmployeeEntity employee,
                                                          List<Path> copiedFilePaths) throws IOException {

        String employeeFolderName = sanitizeFolderName(employeeid);
        Path employeeMediaDirectory = Paths.get(mediaRootDir, employeeFolderName);
        Files.createDirectories(employeeMediaDirectory);

        List<FaceImageEntity> faceImageEntities = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {

            File sourceFile = images.get(i);
            String extension = resolveExtension(sourceFile.getName());
            String fileName = String.format("face_%03d%s", i + 1, extension);

            Path destinationFile = employeeMediaDirectory.resolve(fileName);
            Files.copy(sourceFile.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            copiedFilePaths.add(destinationFile);
            String dbImagePath = String.format("/media/%s/%s", employeeFolderName, fileName);

            FaceImageEntity faceImage = new FaceImageEntity();
            faceImage.setEmployee(employee);
            faceImage.setImagePath(dbImagePath);

            faceImageEntities.add(faceImage);
        }

        return faceImageEntities;
    }

    private String sanitizeFolderName(String folderName) {

        if (folderName == null || folderName.isBlank()) {
            return "unknown_employee";
        }

        return folderName.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String resolveExtension(String filename) {

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex).toLowerCase();
        }

        return ".jpg";
    }

    private void cleanupCopiedFiles(List<Path> copiedFilePaths) {

        for (Path copiedFilePath : copiedFilePaths) {

            try {
                Files.deleteIfExists(copiedFilePath);
            } catch (Exception e) {
                log.warn("Failed to clean copied media file: {}", copiedFilePath);
            }
        }
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