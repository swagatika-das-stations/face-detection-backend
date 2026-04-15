package com.stations.facedetection.integration.kloudspot.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stations.facedetection.integration.kloudspot.DTO.KloudspotRegistrationRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZipBuilder {
    
    private final ObjectMapper objectMapper;

    /**
     * Create a ZIP file containing only image files
     * Structure:
     *   - image1.jpg
     *   - image2.jpg
     *   - ...
     */
    public File createImageZip(List<File> imageFiles) throws IOException {

        File zipFile = File.createTempFile("images_", ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

            // Add all images to ZIP
            for (int i = 0; i < imageFiles.size(); i++) {
                File image = imageFiles.get(i);
                String imageName = "image" + (i + 1) + ".jpg";
                
                ZipEntry zipEntry = new ZipEntry(imageName);
                zos.putNextEntry(zipEntry);

                byte[] bytes = Files.readAllBytes(image.toPath());
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
                
                log.info("Added {} to ZIP: {} bytes", imageName, bytes.length);
            }
        }
        
        long zipSize = zipFile.length();
        log.info("Image ZIP file created: {} bytes ({} MB)", zipSize, String.format("%.2f", zipSize / (1024.0 * 1024.0)));

        return zipFile;
    }
    
    /**
     * Create human.json file with person metadata
     */
    public File createHumanJson(KloudspotRegistrationRequestDTO humanData) throws IOException {
        File jsonFile = File.createTempFile("human_", ".json");
        
        String humanJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(humanData);
        
        Files.write(jsonFile.toPath(), humanJson.getBytes());
        
        log.info("Human.json file created: {} bytes", humanJson.getBytes().length);
        
        return jsonFile;
    }
}