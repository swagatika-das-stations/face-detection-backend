package com.stations.facedetection.Auth.DTO;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDto {
	private String firstName;
    private String lastName;
    private String email;
    private Long  employeeId;

    private String password;
    private String confirmPassword;

    private List<MultipartFile> faceImages;

}
