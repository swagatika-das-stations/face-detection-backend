package com.stations.facedetection.Auth.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stations.facedetection.Auth.DTO.LoginRequestDto;
import com.stations.facedetection.Auth.DTO.LoginResponseDto;
import com.stations.facedetection.Security.Jwt.JwtUtil;
import com.stations.facedetection.User.Entity.UserEntity;
import com.stations.facedetection.User.Repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    // Login
    @Override
    public LoginResponseDto login(LoginRequestDto request) {

        log.info("Login service started for email: {}", request.getEmail());

        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            log.info("Authentication successful for user: {}", request.getEmail());

            UserEntity user = userRepository.findByEmailWithRoles(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("User found in database: {}", user.getEmail());

            String token = jwtUtil.generateToken(user.getEmail());

            log.info("JWT token generated for user: {}", user.getEmail());

            List<String> roles = user.getUserRoles().stream()
                    .map(userRole -> userRole.getRoles().getName())
                    .filter(roleName -> roleName != null && !roleName.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            log.info("Roles assigned to user: {}", roles);

            String primaryRole = resolvePrimaryRole(roles);
            boolean multipleRoles = roles.size() > 1;

            String dashboardPath = resolveDashboardPath(primaryRole, multipleRoles);

            log.info("Primary role resolved: {}", primaryRole);

            return new LoginResponseDto(
                    token,
                    user.getId(),
                    primaryRole,
                    roles,
                    multipleRoles,
                    dashboardPath
            );

        } catch (Exception e) {

            log.error("Login failed for user: {}", request.getEmail(), e);
            throw e;
        }
    }

    private String resolveDashboardPath(String primaryRole, boolean multipleRoles) {

        if (multipleRoles || primaryRole == null) {
            return null;
        }

        String normalizedRole = primaryRole.trim().toUpperCase();

        return switch (normalizedRole) {
            case "ADMIN" -> "/admin/dashboard";
            case "EMPLOYEE" -> "/employee/dashboard";
            default -> null;
        };
    }

    private String resolvePrimaryRole(List<String> roles) {

        if (roles == null || roles.isEmpty()) {
            return null;
        }

        return roles.stream()
                .map(role -> role == null ? null : role.trim().toUpperCase())
                .filter(role -> role != null && !role.isBlank())
                .filter(role -> role.equals("ADMIN"))
                .findFirst()
                .orElse(roles.get(0));
    }
}