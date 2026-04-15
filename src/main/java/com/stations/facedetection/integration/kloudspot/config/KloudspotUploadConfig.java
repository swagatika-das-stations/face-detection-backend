package com.stations.facedetection.integration.kloudspot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "kloudspot.upload")
public class KloudspotUploadConfig {
    
    // Image count limits (as per Kloudspot specs)
    private Integer minImages = 1;
    private Integer maxImages = 5;
    
    // Size limits in MB (as per Kloudspot specs)
    private Integer maxImageSizeMb = 1;  // Max per image in validation
    private Integer maxTotalSizeMb = 5;  // Max total in validation
    private Integer maxImageSizeOptimizedMb = 0;  // 500KB = 0.5MB per image after optimization
    private Integer maxTotalZipSizeMb = 2;  // 2MB total ZIP after optimization
    
    // Supported formats (as per Kloudspot specs)
    private List<String> supportedFormats = List.of("jpg", "jpeg", "png");
    
    // Auto-resize configuration
    private Boolean autoResize = true;
    private Integer targetMaxDimension = 600;  // 600x600 px as per Kloudspot specs
    private Float jpegQuality = 0.85f;  // Reduced quality for smaller files

    /**
     * Get max image size in bytes (for validation)
     */
    public long getMaxImageSizeBytes() {
        return maxImageSizeMb * 1024L * 1024L;
    }

    /**
     * Get max total size in bytes (for validation)
     */
    public long getMaxTotalSizeBytes() {
        return maxTotalSizeMb * 1024L * 1024L;
    }
    
    /**
     * Get max optimized image size in bytes (500KB per image)
     */
    public long getMaxOptimizedImageSizeBytes() {
        return 500 * 1024L;  // 500KB
    }
    
    /**
     * Get max total ZIP size in bytes (2MB)
     */
    public long getMaxTotalZipSizeBytes() {
        return maxTotalZipSizeMb * 1024L * 1024L;
    }
    
    /**
     * Get max ZIP size in bytes (2MB total)
     */
    public long getMaxZipSizeBytes() {
        return maxTotalZipSizeMb * 1024L * 1024L;
    }

    /**
     * Check if a format is supported (case-insensitive)
     */
    public boolean isFormatSupported(String format) {
        if (format == null) return false;
        return supportedFormats.stream()
                .anyMatch(supported -> supported.equalsIgnoreCase(format));
    }
}