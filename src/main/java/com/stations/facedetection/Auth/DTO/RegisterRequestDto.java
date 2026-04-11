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


    //registered data storing in employee entity
	private String firstName;
    private String lastName;
    private Long  employeeId;
    //email and paasword is storing in user entity
    private String email;
    private String password;
    private String confirmPassword;
//image is storing in FaceRegistryEntity, 
    private List<MultipartFile> faceImages;

}
