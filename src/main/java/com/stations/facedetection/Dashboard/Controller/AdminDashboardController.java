package com.stations.facedetection.Dashboard.Controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stations.facedetection.Dashboard.DTO.AttendanceCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.EmployeeCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.UnknownAlertsResponseDto;
import com.stations.facedetection.Dashboard.Service.AdminDashboardService;
import com.stations.facedetection.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/checkin")
    public ResponseEntity<ApiResponse> getCheckinEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okAttendance("Check-in employees fetched successfully", adminDashboardService.getCheckins(date));
    }

    @GetMapping("/checkin/date={date}")
    public ResponseEntity<ApiResponse> getCheckinEmployeesByPath(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okAttendance("Check-in employees fetched successfully", adminDashboardService.getCheckins(date));
    }

    @GetMapping("/checkout")
    public ResponseEntity<ApiResponse> getCheckoutEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okAttendance("Check-out employees fetched successfully", adminDashboardService.getCheckouts(date));
    }

    @GetMapping("/checkout/date={date}")
    public ResponseEntity<ApiResponse> getCheckoutEmployeesByPath(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okAttendance("Check-out employees fetched successfully", adminDashboardService.getCheckouts(date));
    }

    @GetMapping("/headcount")
    public ResponseEntity<ApiResponse> getHeadcount(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okAttendance("Headcount fetched successfully", adminDashboardService.getHeadcount(date));
    }

    @GetMapping("/headcount/date={date}")
    public ResponseEntity<ApiResponse> getHeadcountByPath(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okAttendance("Headcount fetched successfully", adminDashboardService.getHeadcount(date));
    }

    @GetMapping("/total-employees")
    public ResponseEntity<ApiResponse> getTotalEmployees() {

        EmployeeCardResponseDto payload = adminDashboardService.getTotalEmployees();
        return ResponseEntity.ok(new ApiResponse(true, "Total employees fetched successfully", payload));
    }

    @GetMapping("/on-leave")
    public ResponseEntity<ApiResponse> getOnLeaveEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okEmployeeCard("On-leave employees fetched successfully", adminDashboardService.getOnLeave(date));
    }

    @GetMapping("/on-leave/date={date}")
    public ResponseEntity<ApiResponse> getOnLeaveEmployeesByPath(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okEmployeeCard("On-leave employees fetched successfully", adminDashboardService.getOnLeave(date));
    }

    @GetMapping("/unknown-alerts")
    public ResponseEntity<ApiResponse> getUnknownAlerts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okUnknownAlerts("Unknown alerts fetched successfully", adminDashboardService.getUnknownAlerts(date));
    }

    @GetMapping("/unknown-alerts/date={date}")
    public ResponseEntity<ApiResponse> getUnknownAlertsByPath(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return okUnknownAlerts("Unknown alerts fetched successfully", adminDashboardService.getUnknownAlerts(date));
    }

    private ResponseEntity<ApiResponse> okAttendance(String message, AttendanceCardResponseDto payload) {
        return ResponseEntity.ok(new ApiResponse(true, message, payload));
    }

    private ResponseEntity<ApiResponse> okEmployeeCard(String message, EmployeeCardResponseDto payload) {
        return ResponseEntity.ok(new ApiResponse(true, message, payload));
    }

    private ResponseEntity<ApiResponse> okUnknownAlerts(String message, UnknownAlertsResponseDto payload) {
        return ResponseEntity.ok(new ApiResponse(true, message, payload));
    }
}
