package com.stations.facedetection.Auth.Controller;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stations.facedetection.common.response.ErrorResponse;
import com.stations.facedetection.integration.kloudspot.DTO.RegistrationResponseDto;
import com.stations.facedetection.integration.kloudspot.service.KloudspotFaceRegistrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/faces")
@RequiredArgsConstructor
public class FaceRegistrationController {

    private final KloudspotFaceRegistrationService service;

    @Value("${kloudspot.upload.min-images:1}")
    private int minImages;

    @Value("${kloudspot.upload.max-images:5}")
    private int maxImages;

    @Value("${kloudspot.upload.max-image-size-mb:1}")
    private int maxImageSizeMb;

    @Value("${kloudspot.upload.max-total-size-mb:5}")
    private int maxTotalSizeMb;

    @Value("${kloudspot.upload.supported-formats:}")
    private List<String> supportedFormats;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam("faceImages") MultipartFile[] images,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String employeeid,
            @RequestParam String password) {

        log.info("Face registration API called");
        log.info("Request details: firstName={}, lastName={}, email={}, employeeId={}, imageCount={}",
                firstName, lastName, email, employeeid, images != null ? images.length : 0);

        if (images != null) {
            for (MultipartFile image : images) {
                log.info("Incoming image: filename={}, sizeBytes={}, contentType={}",
                        image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
        }

        ResponseEntity<ErrorResponse> validationError = validateUpload(images);

        if (validationError != null) {
            log.warn("Image validation failed");
            return validationError;
        }

        try {

            log.info("Converting MultipartFiles to File objects");

            List<File> files = Arrays.stream(images)
                    .map(this::convertToFile)
                    .toList();

            log.info("Calling Kloudspot registration service");

            RegistrationResponseDto response = service.registerPerson(
                    files, firstName, lastName, email, employeeid, password);

            log.info("Received response from Kloudspot: {}", response);

            files.forEach(File::delete);
            log.info("Temporary files deleted");

                        if (response == null) {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Map.of("error", "Registration failed", "message", "No response from registration service"));
                        }

                        if ("failure".equalsIgnoreCase(response.getStatus())) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                                .body(Map.of("error", "Registration failed", "message", response.getMessage()));
                        }

            if ("ALREADY_EXISTS".equalsIgnoreCase(response.getStatus())) {

                log.warn("Person already exists in Kloudspot");

                                return ResponseEntity.ok(Map.of("status", "successful", "message", response.getMessage()));
            }

            log.info("Face registration successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.error("Error occurred during face registration", e);

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Registration failed", "message", e.getMessage()));
        }
    }

    private ResponseEntity<ErrorResponse> validateUpload(MultipartFile[] images) {

        log.info("Validating uploaded images");

        if (images == null || images.length == 0) {

            log.warn("No images uploaded");

            return new ResponseEntity<>(
                    new ErrorResponse("At least one image is required", "INVALID_UPLOAD"),
                    HttpStatus.BAD_REQUEST);
        }

        if (images.length < minImages || images.length > maxImages) {

            log.warn("Invalid image count: {}", images.length);

            return new ResponseEntity<>(
                    new ErrorResponse(
                            "Image count must be between " + minImages + " and " + maxImages,
                            "INVALID_UPLOAD"),
                    HttpStatus.BAD_REQUEST);
        }

        long maxImageSizeBytes = maxImageSizeMb * 1024L * 1024L;
        long maxTotalSizeBytes = maxTotalSizeMb * 1024L * 1024L;
        long totalSize = 0L;

        Set<String> allowedFormats = supportedFormats == null ?
                Set.of() :
                supportedFormats.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

        for (MultipartFile file : images) {

            long size = file.getSize();
            totalSize += size;

            log.info("Checking image: {} size: {} bytes",
                    file.getOriginalFilename(), size);

            if (size > maxImageSizeBytes) {

                log.warn("Image exceeds max size limit");

                return new ResponseEntity<>(
                        new ErrorResponse(
                                "Each image must be <= " + maxImageSizeMb + "MB",
                                "INVALID_UPLOAD"),
                        HttpStatus.BAD_REQUEST);
            }

            if (!allowedFormats.isEmpty()) {

                String filename = file.getOriginalFilename();
                String extension = filename == null ? "" :
                        filename.substring(filename.lastIndexOf(".") + 1);

                if (!allowedFormats.contains(extension.toLowerCase())) {

                    log.warn("Invalid image format: {}", extension);

                    return new ResponseEntity<>(
                            new ErrorResponse(
                                    "Only these formats are allowed: "
                                            + String.join(", ", allowedFormats),
                                    "INVALID_UPLOAD"),
                            HttpStatus.BAD_REQUEST);
                }
            }
        }

        if (totalSize > maxTotalSizeBytes) {

            log.warn("Total upload size exceeded");

            return new ResponseEntity<>(
                    new ErrorResponse(
                            "Total upload size must be <= " + maxTotalSizeMb + "MB",
                            "INVALID_UPLOAD"),
                    HttpStatus.BAD_REQUEST);
        }

        log.info("Image validation successful");

        return null;
    }

    private File convertToFile(MultipartFile file) {

        try {

            log.info("Converting file: {}", file.getOriginalFilename());

            String originalName = file.getOriginalFilename();
            String extension = originalName.substring(originalName.lastIndexOf("."));

            File convFile = File.createTempFile("upload_", extension);
            file.transferTo(convFile);

            return convFile;

        } catch (Exception e) {

            log.error("File conversion failed", e);

            throw new RuntimeException("File conversion failed", e);
        }
    }
}