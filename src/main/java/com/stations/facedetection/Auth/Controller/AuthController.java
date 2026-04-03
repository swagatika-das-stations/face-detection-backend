package com.stations.facedetection.Auth.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stations.facedetection.Auth.DTO.LoginRequestDto;
import com.stations.facedetection.Auth.DTO.LoginResponseDto;
import com.stations.facedetection.Auth.DTO.RegisterRequestDto;
import com.stations.facedetection.Auth.Service.AuthService;
import com.stations.facedetection.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
	//Autowired classes...
	private final AuthService authService;

	// Register API (Face detection.....)

	
    // Login API
	@PostMapping("/login")
	public ResponseEntity<ApiResponse> login(@RequestBody LoginRequestDto request) {

	    LoginResponseDto loginResponse = authService.login(request);

	    ApiResponse response = new ApiResponse(
	            true,
	            "Login successful",
	            loginResponse
	    );

	    return ResponseEntity.ok(response);
	}
}
