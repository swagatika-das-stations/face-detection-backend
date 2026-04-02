package com.stations.facedetection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FaceDetectionServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(FaceDetectionServicesApplication.class, args);
	}

}
