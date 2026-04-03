package com.stations.facedetection.integration.kloudspot.service;

import com.stations.facedetection.integration.kloudspot.config.KloudspotUploadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageValidator {

    private final KloudspotUploadConfig uploadConfig;

    /**
     * Validate images according to Kloudspot specifications
     */
    public void validateImages(List<File> images) {
        log.info("Validating {} images against Kloudspot specifications", images.size());

        // 1. Check number of images
        validateImageCount(images);

        // 2. Validate each image
        long totalSize = 0;
        for (int i = 0; i < images.size(); i++) {
            File image = images.get(i);
            log.info("--- Validating Image {} of {} ---", i + 1, images.size());
            
            validateSingleImage(image, i + 1);
            totalSize += image.length();
        }

        // 3. Check total size
        validateTotalSize(totalSize);

        log.info("All images validated successfully!");
    }

    private void validateImageCount(List<File> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException(
                "No images provided. Minimum required: " + uploadConfig.getMinImages()
            );
        }

        if (images.size() < uploadConfig.getMinImages()) {
            throw new IllegalArgumentException(
                String.format("Too few images. Provided: %d, Minimum required: %d",
                    images.size(), uploadConfig.getMinImages())
            );
        }

        if (images.size() > uploadConfig.getMaxImages()) {
            throw new IllegalArgumentException(
                String.format("Too many images. Provided: %d, Maximum allowed: %d",
                    images.size(), uploadConfig.getMaxImages())
            );
        }

        log.info("Image count validation passed: {} images", images.size());
    }

    private void validateSingleImage(File image, int imageNumber) {
        // Check if file exists
        if (!image.exists()) {
            throw new IllegalArgumentException("Image " + imageNumber + " does not exist");
        }

        // Check file size
        long fileSize = image.length();
        long maxSize = uploadConfig.getMaxImageSizeBytes();

        log.info("Image {}: {} ({} KB)", 
            imageNumber, 
            image.getName(), 
            fileSize / 1024);

        if (fileSize > maxSize) {
            if (uploadConfig.getAutoResize()) {
                log.warn("Image {} is {} MB (max: {} MB) - Will be auto-resized",
                    imageNumber,
                    String.format("%.2f", fileSize / (1024.0 * 1024.0)),
                    uploadConfig.getMaxImageSizeMb());
            } else {
                throw new IllegalArgumentException(
                    String.format("Image %d is too large: %.2f MB (max: %d MB)",
                        imageNumber,
                        fileSize / (1024.0 * 1024.0),
                        uploadConfig.getMaxImageSizeMb())
                );
            }
        }

        // Check format
        String fileName = image.getName().toLowerCase();
        String extension = getFileExtension(fileName);

        if (!uploadConfig.isFormatSupported(extension)) {
            throw new IllegalArgumentException(
                String.format("Image %d has unsupported format: %s (supported: %s)",
                    imageNumber,
                    extension,
                    String.join(", ", uploadConfig.getSupportedFormats()))
            );
        }

        log.info("Format: {} ✅", extension.toUpperCase());

        // Try to read image
        validateImageReadable(image, imageNumber);

        log.info("Image {} validation passed", imageNumber);
    }

    private void validateImageReadable(File image, int imageNumber) {
        try {
            BufferedImage img = ImageIO.read(image);
            if (img == null) {
                throw new IllegalArgumentException(
                    "Image " + imageNumber + " is corrupted or invalid"
                );
            }

            int width = img.getWidth();
            int height = img.getHeight();

            log.info("Dimensions: {}x{}", width, height);

            // Check minimum dimensions (at least 100x100 for face recognition)
            if (width < 100 || height < 100) {
                throw new IllegalArgumentException(
                    String.format("Image %d is too small: %dx%d (minimum: 100x100)",
                        imageNumber, width, height)
                );
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Failed to read image " + imageNumber + ": " + e.getMessage(), e
            );
        }
    }

    private void validateTotalSize(long totalSize) {
        long maxTotalSize = uploadConfig.getMaxTotalSizeBytes();
        
        if (totalSize > maxTotalSize) {
            throw new IllegalArgumentException(
                String.format("Total images size too large: %.2f MB (max: %.2f MB)",
                    totalSize / (1024.0 * 1024.0),
                    maxTotalSize / (1024.0 * 1024.0))
            );
        }

        log.info("Total size validation passed: {} MB", 
            String.format("%.2f", totalSize / (1024.0 * 1024.0)));
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}