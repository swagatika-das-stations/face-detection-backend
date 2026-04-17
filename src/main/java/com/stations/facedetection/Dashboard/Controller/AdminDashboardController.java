package com.stations.facedetection.Dashboard.Controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stations.facedetection.Dashboard.DTO.AttendanceCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.EmployeeCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.UnknownAlertsResponseDto;
import com.stations.facedetection.Dashboard.Service.AdminDashboardService;
import com.stations.facedetection.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    // ================= CHECK-IN =================

    @GetMapping("/checkin")
    public ResponseEntity<ApiResponse> getCheckinEmployees(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("API Request → GET /checkin , date={}", date);

        return okAttendance(
                "Check-in employees fetched successfully",
                adminDashboardService.getCheckins(date)
        );
    }

    // ================= CHECK-OUT =================

    @GetMapping("/checkout")
    public ResponseEntity<ApiResponse> getCheckoutEmployees(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("API Request → GET /checkout , date={}", date);

        return okAttendance(
                "Check-out employees fetched successfully",
                adminDashboardService.getCheckouts(date)
        );
    }

    // ================= HEADCOUNT =================

    @GetMapping("/headcount")
    public ResponseEntity<ApiResponse> getHeadcount(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("API Request → GET /headcount , date={}", date);

        return okAttendance(
                "Headcount fetched successfully",
                adminDashboardService.getHeadcount(date)
        );
    }

    // ================= TOTAL EMPLOYEES =================

    @GetMapping("/total-employees")
    public ResponseEntity<ApiResponse> getTotalEmployees() {

        log.info("API Request → GET /total-employees");

        EmployeeCardResponseDto payload = adminDashboardService.getTotalEmployees();

        log.info("Total employees count={}", payload.getTotalCount());

        return ResponseEntity.ok(
                new ApiResponse(true, "Total employees fetched successfully", payload)
        );
    }

    // ================= ON LEAVE =================

    @GetMapping("/on-leave")
    public ResponseEntity<ApiResponse> getOnLeaveEmployees(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("API Request → GET /on-leave , date={}", date);

        return okEmployeeCard(
                "On-leave employees fetched successfully",
                adminDashboardService.getOnLeave(date)
        );
    }

    @GetMapping("/present-employees")
    public ResponseEntity<ApiResponse> getPresentEmployees(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("API Request → GET /present-employees , date={}", date);

        return okEmployeeCard(
                "Present employees fetched successfully",
                adminDashboardService.getPresentEmployees(date)
        );
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse> getEmployeesByScope(
            @RequestParam(required = false, defaultValue = "total") String scope,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("API Request → GET /employees , scope={}, date={}", scope, date);

        return okEmployeeCard(
                "Employees fetched successfully",
                adminDashboardService.getEmployeesByScope(scope, date)
        );
    }

    // ================= UNKNOWN ALERTS =================

    @GetMapping("/unknown-alerts")
    public ResponseEntity<ApiResponse> getUnknownAlerts(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("API Request → GET /unknown-alerts , date={}", date);

        return okUnknownAlerts(
                "Unknown alerts fetched successfully",
                adminDashboardService.getUnknownAlerts(date)
        );
    }

    // ================= RESPONSE HELPERS =================

    private ResponseEntity<ApiResponse> okAttendance(
            String message,
            AttendanceCardResponseDto payload) {

        log.info("Dashboard response → message='{}', date={}, count={}",
                message, payload.getDate(), payload.getTotalCount());

        return ResponseEntity.ok(
                new ApiResponse(true, message, payload)
        );
    }

    private ResponseEntity<ApiResponse> okEmployeeCard(
            String message,
            EmployeeCardResponseDto payload) {

        log.info("Dashboard response → message='{}', date={}, count={}",
                message, payload.getDate(), payload.getTotalCount());

        return ResponseEntity.ok(
                new ApiResponse(true, message, payload)
        );
    }

    private ResponseEntity<ApiResponse> okUnknownAlerts(
            String message,
            UnknownAlertsResponseDto payload) {

        log.info("Dashboard response → message='{}', date={}, count={}",
                message, payload.getDate(), payload.getTotalCount());

        return ResponseEntity.ok(
                new ApiResponse(true, message, payload)
        );
    }
}