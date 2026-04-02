package com.stations.facedetection.Auth.Controller;

import java.io.File;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stations.facedetection.integration.kloudspot.DTO.RegistrationResponseDto;
import com.stations.facedetection.integration.kloudspot.service.KloudspotFaceRegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/faces")
@RequiredArgsConstructor
public class FaceRegistrationController {

	private final KloudspotFaceRegistrationService service;

	@PostMapping("/register")
	public RegistrationResponseDto register(@RequestParam("faceImages") List<MultipartFile> images,
		 @RequestParam String firstName, @RequestParam String lastName,
			@RequestParam String email, @RequestParam String employeeId) throws Exception {

		// Convert MultipartFile → File
		List<File> files = images.stream().map(this::convertToFile).toList();

		return service.registerPerson(files, firstName, lastName, email, employeeId);
	}

	private File convertToFile(MultipartFile file) {
		try {
			String originalName = file.getOriginalFilename();
			String extension = originalName.substring(originalName.lastIndexOf("."));

			File convFile = File.createTempFile("upload_", extension);
			file.transferTo(convFile);

			return convFile;
		} catch (Exception e) {
			throw new RuntimeException("File conversion failed", e);
		}
	}
}