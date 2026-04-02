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
    private Integer maxImageSizeMb = 1;
    private Integer maxTotalSizeMb = 5;
    
    // Supported formats (as per Kloudspot specs)
    private List<String> supportedFormats = List.of("jpg", "jpeg", "png");
    
    // Auto-resize configuration
    private Boolean autoResize = true;
    private Integer targetMaxDimension = 1920;
    private Float jpegQuality = 0.90f;

    /**
     * Get max image size in bytes
     */
    public long getMaxImageSizeBytes() {
        return maxImageSizeMb * 1024L * 1024L;
    }

    /**
     * Get max total size in bytes
     */
    public long getMaxTotalSizeBytes() {
        return maxTotalSizeMb * 1024L * 1024L;
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