package com.stations.facedetection.integration.kloudspot.service;

import com.stations.facedetection.integration.kloudspot.config.KloudspotUploadConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleaner {

    private final KloudspotUploadConfig uploadConfig;

    /**
     * Clean and optimize image for Kloudspot
     * Specifications:
     * - Dimensions: 600x600 px (1:1 square aspect ratio)
     * - File Size: less than 500KB per image
     * - Format: JPEG with optimized quality
     */
    public File cleanImage(File originalImage) throws IOException {
        
        log.info("Cleaning image: {}", originalImage.getName());
        
        long originalSize = originalImage.length();
        long maxOptimizedSize = uploadConfig.getMaxOptimizedImageSizeBytes();

        log.info("Original size: {} KB (target: {} KB)", 
                originalSize / 1024, 
                maxOptimizedSize / 1024);
        
        BufferedImage original = ImageIO.read(originalImage);
        if (original == null) {
            throw new IOException("Failed to read image: " + originalImage.getName());
        }

        int width = original.getWidth();
        int height = original.getHeight();
        
        log.info("Original dimensions: {}x{}", width, height);

        // 1. Ensure square 600x600 px (1:1 aspect ratio)
        BufferedImage square = cropToSquare(original);
        BufferedImage resized = resizeImage(square, 600);
        
        // 2. Convert to RGB
        BufferedImage rgb = convertToRgb(resized);

        // 3. Save as optimized JPEG
        File cleanedFile = saveAsOptimizedJpeg(rgb, uploadConfig.getJpegQuality());
        long cleanedSize = cleanedFile.length();

        log.info("After first compression: {} KB (target: {} KB)", 
                cleanedSize / 1024, 
                maxOptimizedSize / 1024);

        // 4. If still too large, reduce quality until it fits
        if (cleanedSize > maxOptimizedSize) {
            log.warn("Image larger than 500KB, reducing quality");
            cleanedFile = reduceQualityUntilFits(rgb, maxOptimizedSize);
        }

        long finalSize = cleanedFile.length();
        
        log.info("Optimized: {} KB to {} KB ({}% reduction)", 
                originalSize / 1024, 
                finalSize / 1024,
                (int)(100 - (finalSize * 100.0 / originalSize)));
        
        if (finalSize > maxOptimizedSize) {
            throw new IOException(
                String.format("Cannot compress image to 500KB. Current size: %d KB", finalSize / 1024)
            );
        }

        return cleanedFile;
    }
    
    /**
     * Crop image to square (1:1 aspect ratio) from center
     */
    private BufferedImage cropToSquare(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        
        BufferedImage cropped = original.getSubimage(x, y, size, size);
        log.info("Cropped to square: {}x{}", size, size);
        return cropped;
    }

    private File optimizeFormat(BufferedImage image, String originalName) throws IOException {
        BufferedImage rgb = convertToRgb(image);
        return saveAsOptimizedJpeg(rgb, uploadConfig.getJpegQuality());
    }

    private BufferedImage resizeImage(BufferedImage original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();

        int newWidth, newHeight;
        if (width > height) {
            newWidth = maxDimension;
            newHeight = (int) ((double) height / width * maxDimension);
        } else {
            newHeight = maxDimension;
            newWidth = (int) ((double) width / height * maxDimension);
        }

        log.info("Resizing to: {}x{}", newWidth, newHeight);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resized;
    }

    private BufferedImage convertToRgb(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(
                image.getWidth(), 
                image.getHeight(), 
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private File saveAsOptimizedJpeg(BufferedImage image, float quality) throws IOException {
        File tempFile = File.createTempFile("optimized_", ".jpg");
        
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
            
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                throw new IOException("No JPEG writer found");
            }

            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }

            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
        }

        return tempFile;
    }

    private File reduceQualityUntilFits(BufferedImage image, long maxSize) throws IOException {
        float quality = uploadConfig.getJpegQuality();
        File tempFile = null;
        
        while (quality > 0.5f) {
            quality -= 0.1f;
            tempFile = saveAsOptimizedJpeg(image, quality);
            
            if (tempFile.length() <= maxSize) {
                log.info("Reduced quality to {}% - Size: {} KB", 
                        (int)(quality * 100), 
                        tempFile.length() / 1024);
                return tempFile;
            }
        }
        
        if (tempFile == null || tempFile.length() > maxSize) {
            throw new IOException(
                    String.format("Cannot reduce image to required size. Min achievable: %d KB, Required: %d KB",
                            tempFile != null ? tempFile.length() / 1024 : 0,
                            maxSize / 1024)
            );
        }
        
        return tempFile;
    }
}