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

        // Build audit record — always saved regardless of outcome
        FaceRegistryEntity audit = buildAttemptHistory(firstName, lastName, email, employeeid);

        try {
            log.info("Starting face registration for employeeId={}, email={}", employeeid, email);

            // ── Already exists in Kloudspot ──────────────────────────────────────
            if (searchService.checkIdentityExists(email)) {
                log.warn("Identity already exists in Kloudspot for email={}", email);
                audit.setRegistrationStatus("ALREADY_EXISTS");
                audit.setFailureReason("Identity already exists in Kloudspot");
                tryPersistAudit(audit);

                RegistrationResponseDto resp = new RegistrationResponseDto();
                resp.setStatus("successful");
                resp.setMessage("Identity already exists in Kloudspot");
                return resp;
            }

            // ── Validate & clean images ──────────────────────────────────────────
            imageValidator.validateImages(images);
            for (File img : images) {
                cleanedImages.add(imageCleaner.cleanImage(img));
            }

            // ── Build zip & json ─────────────────────────────────────────────────
            humanJsonFile = zipBuilder.createHumanJson(buildRequest(firstName, lastName, email, employeeid));
            imageZipFile  = zipBuilder.createImageZip(cleanedImages);

            long zipSize = imageZipFile.length();
            long maxZipSize = uploadConfig.getMaxTotalZipSizeBytes();
            if (zipSize > maxZipSize) {
                throw new RuntimeException(String.format(
                    "Image ZIP exceeds max size. Current: %d KB, Max: %d KB",
                    zipSize / 1024, maxZipSize / 1024));
            }

            // ── Call Kloudspot ───────────────────────────────────────────────────
            RegistrationResponseDto response = registrationService.register(humanJsonFile, imageZipFile);

            if (response == null) {
                audit.setRegistrationStatus("FAILED");
                audit.setFailureReason("No response from Kloudspot");
                tryPersistAudit(audit);

                RegistrationResponseDto err = new RegistrationResponseDto();
                err.setStatus("failure");
                err.setMessage("Registration failed: no response from Kloudspot");
                return err;
            }

            String entityId = response.getEntityId();
            boolean hasEntityId = StringUtils.hasText(entityId);

            // ── Success WITH entityId → save employee ────────────────────────────
            if ("successful".equalsIgnoreCase(response.getStatus()) && hasEntityId) {
                log.info("Registration successful. EntityId={}", entityId);
                saveEmployee(firstName, lastName, email, employeeid, password, images, entityId, audit);
                return response;
            }

            // ── Success WITHOUT entityId → audit only, no employee record ────────
            if ("successful".equalsIgnoreCase(response.getStatus())) {
                log.warn("Kloudspot returned success but no entityId — not saving employee record");
                audit.setRegistrationStatus("FAILED");
                audit.setFailureReason("No entityId returned from Kloudspot");
                tryPersistAudit(audit);

                response.setStatus("failure");
                response.setMessage("Registration failed: no entity ID returned");
                return response;
            }

            // ── Any other failure ────────────────────────────────────────────────
            log.error("Kloudspot registration failed. Response={}", response);
            audit.setRegistrationStatus("FAILED");
            audit.setFailureReason(response.getMessage());
            tryPersistAudit(audit);
            response.setStatus("failure");
            return response;

        } catch (IllegalArgumentException e) {
            log.error("Validation failed: {}", e.getMessage());
            audit.setRegistrationStatus("FAILED");
            audit.setFailureReason(e.getMessage());
            tryPersistAudit(audit);

            RegistrationResponseDto err = new RegistrationResponseDto();
            err.setStatus("failure");
            err.setMessage(e.getMessage());
            return err;

        } catch (Exception e) {
            log.error("Registration process failed", e);
            audit.setRegistrationStatus("FAILED");
            audit.setFailureReason(e.getMessage());
            tryPersistAudit(audit);

            RegistrationResponseDto err = new RegistrationResponseDto();
            err.setStatus("failure");
            err.setMessage("Registration failed: " + e.getMessage());
            return err;

        } finally {
            cleanupTempFile(imageZipFile);
            cleanupTempFile(humanJsonFile);
            cleanedImages.forEach(this::cleanupTempFile);
        }
    }

    /**
     * Saves employee, user, face images to DB.
     * Only called when entityId is confirmed non-null.
     */
    private void saveEmployee(String firstName, String lastName, String email,
                               String employeeid, String password, List<File> images,
                               String entityId, FaceRegistryEntity audit) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUserWithRole(email, password));

        EmployeeEntity employee = resolveEmployeeForUser(user, firstName, lastName, employeeid);
        employee.setEntityId(entityId);
        employee = employeeRepository.save(employee);

        try {
            List<FaceImageEntity> faceImages = copyAndBuildFaceImages(images, employee);
            faceImageRepository.saveAll(faceImages);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist face images", e);
        }

        audit.setUser(user);
        audit.setEmployee(employee);
        audit.setEntityId(entityId);
        audit.setRegistrationStatus("SUCCESS");
        audit.setFailureReason(null);
        tryPersistAudit(audit);

        log.info("Employee saved: email={}, entityId={}", email, entityId);
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

    private void tryPersistAudit(FaceRegistryEntity audit) {
        try {
            faceRegistryRepository.save(audit);
        } catch (Exception ex) {
            log.warn("Failed to persist audit record: {}", ex.getMessage());
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