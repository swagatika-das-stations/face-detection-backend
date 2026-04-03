package com.stations.facedetection.Auth.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String token;
    private Long userId;
    private String role;
    private List<String> roles;
    private boolean multipleRoles;
    private String dashboardPath;
}
