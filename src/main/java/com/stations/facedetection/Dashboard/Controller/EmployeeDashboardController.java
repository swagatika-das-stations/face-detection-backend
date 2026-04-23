package com.stations.facedetection.Dashboard.Controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stations.facedetection.Dashboard.DTO.EmployeeCheckinCheckoutDashboardDto;
import com.stations.facedetection.Dashboard.Service.EmployeeCheckinCheckoutService;
import com.stations.facedetection.User.Repository.EmployeeRepository;
import com.stations.facedetection.User.Repository.FaceImageRepository;
import com.stations.facedetection.User.Repository.UserRepository;
import com.stations.facedetection.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/employee/dashboard")
@RequiredArgsConstructor
@Slf4j
public class EmployeeDashboardController {

    private final EmployeeCheckinCheckoutService employeeCheckinCheckoutService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final FaceImageRepository faceImageRepository;

    @GetMapping("/checkin-checkout")
    public ResponseEntity<ApiResponse> getCheckinCheckoutDashboard(
            Principal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String email = principal.getName();

        log.info("Employee dashboard API called → /checkin-checkout");
        log.info("Request details → user={}, startDate={}, endDate={}", email, startDate, endDate);

        try {

            EmployeeCheckinCheckoutDashboardDto dashboardData =
                    employeeCheckinCheckoutService.getDashboard(email, startDate, endDate);

            log.info("Dashboard data fetched successfully for user={}", email);

            ApiResponse response = new ApiResponse(
                    true,
                    "Check-in/check-out dashboard data fetched successfully",
                    dashboardData
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.error("Error fetching dashboard data for user={}", email, e);
            throw e;
        }
    }

    @GetMapping("/profile-image")
    public ResponseEntity<ApiResponse> getProfileImage(Principal principal) {

        String email = principal.getName();
        log.info("Profile image requested for user={}", email);

        String base64Image = userRepository.findByEmail(email)
                .flatMap(user -> employeeRepository.findByUserId(user.getId()))
                .flatMap(emp -> faceImageRepository.findFirstByEmployeeId(emp.getId()).stream().findFirst())
                .filter(img -> img.getImageData() != null)
                .map(img -> "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(img.getImageData()))
                .orElse(null);

        return ResponseEntity.ok(new ApiResponse(true, "Profile image fetched", Map.of("profileImage", base64Image != null ? base64Image : "")));
    }
}