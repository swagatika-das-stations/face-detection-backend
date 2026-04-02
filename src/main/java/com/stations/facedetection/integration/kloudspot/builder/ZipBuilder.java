package com.stations.facedetection.integration.kloudspot.builder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Component;

@Component
public class ZipBuilder {

    public File createZip(List<File> imageFiles) throws IOException {

        File zipFile = File.createTempFile("faces_", ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

            for (File image : imageFiles) {

                ZipEntry zipEntry = new ZipEntry("images/" + image.getName());
                zos.putNextEntry(zipEntry);

                byte[] bytes = Files.readAllBytes(image.toPath());
                zos.write(bytes, 0, bytes.length);

                zos.closeEntry();
            }
        }

        return zipFile;
    }
}