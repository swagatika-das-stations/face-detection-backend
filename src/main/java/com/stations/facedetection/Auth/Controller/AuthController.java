
package com.stations.facedetection.Auth.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stations.facedetection.Auth.DTO.LoginRequestDto;
import com.stations.facedetection.Auth.DTO.LoginResponseDto;
import com.stations.facedetection.Auth.Service.AuthService;
import com.stations.facedetection.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/auth")
public class AuthController {

    private final AuthService authService;

    // Login API
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequestDto request) {

        log.info("Login API called");
        log.info("Login attempt for user: {}", request.getEmail());

        try {

            LoginResponseDto loginResponse = authService.login(request);

            log.info("Login successful for user: {}", request.getEmail());

            ApiResponse response = new ApiResponse(
                    true,
                    "Login successful",
                    loginResponse
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.error("Login failed for user: {}", request.getEmail(), e);

            throw e;
        }
    }
}