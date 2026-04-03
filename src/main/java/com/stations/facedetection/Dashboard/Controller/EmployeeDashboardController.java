package com.stations.facedetection.Dashboard.Controller;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stations.facedetection.Dashboard.DTO.EmployeeCheckinCheckoutDashboardDto;
import com.stations.facedetection.Dashboard.Service.EmployeeCheckinCheckoutService;
import com.stations.facedetection.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/employee/dashboard")
@RequiredArgsConstructor
public class EmployeeDashboardController {

    private final EmployeeCheckinCheckoutService employeeCheckinCheckoutService;

    @GetMapping("/checkin-checkout")
    public ResponseEntity<ApiResponse> getCheckinCheckoutDashboard(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        EmployeeCheckinCheckoutDashboardDto dashboardData = employeeCheckinCheckoutService
                .getDashboard(principal.getName(), startDate, endDate);

        ApiResponse response = new ApiResponse(
                true,
                "Check-in/check-out dashboard data fetched successfully",
                dashboardData);

        return ResponseEntity.ok(response);
    }
}
