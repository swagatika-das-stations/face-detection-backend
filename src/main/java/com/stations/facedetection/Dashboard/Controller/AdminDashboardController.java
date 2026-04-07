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
import com.stations.facedetection.Dashboard.DTO.ProcedureEmployeeDirectoryDto;
import com.stations.facedetection.Dashboard.DTO.UnknownAlertsResponseDto;
import com.stations.facedetection.Dashboard.Service.AdminDashboardService;
import com.stations.facedetection.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/checkin")
    public ResponseEntity<ApiResponse> getCheckinEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /checkin with date={}", date);

        return okAttendance("Check-in employees fetched successfully", adminDashboardService.getCheckins(date));
    }

    @GetMapping("/checkin/date={date}")
    public ResponseEntity<ApiResponse> getCheckinEmployeesByPath(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /checkin/date={} (path style)", date);

        return okAttendance("Check-in employees fetched successfully", adminDashboardService.getCheckins(date));
    }

    @GetMapping("/checkout")
    public ResponseEntity<ApiResponse> getCheckoutEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /checkout with date={}", date);

        return okAttendance("Check-out employees fetched successfully", adminDashboardService.getCheckouts(date));
    }

    @GetMapping("/checkout/date={date}")
    public ResponseEntity<ApiResponse> getCheckoutEmployeesByPath(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /checkout/date={} (path style)", date);

        return okAttendance("Check-out employees fetched successfully", adminDashboardService.getCheckouts(date));
    }

    @GetMapping("/headcount")
    public ResponseEntity<ApiResponse> getHeadcount(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /headcount with date={}", date);

        return okAttendance("Headcount fetched successfully", adminDashboardService.getHeadcount(date));
    }

    @GetMapping("/headcount/date={date}")
    public ResponseEntity<ApiResponse> getHeadcountByPath(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /headcount/date={} (path style)", date);

        return okAttendance("Headcount fetched successfully", adminDashboardService.getHeadcount(date));
    }

    @GetMapping("/total-employees")
    public ResponseEntity<ApiResponse> getTotalEmployees() {

        log.info("Dashboard API called: /total-employees");

        EmployeeCardResponseDto payload = adminDashboardService.getTotalEmployees();
        return ResponseEntity.ok(new ApiResponse(true, "Total employees fetched successfully", payload));
    }

    @GetMapping("/employee-directory")
    public ResponseEntity<ApiResponse> getEmployeeDirectoryFromProcedure() {

        log.info("Dashboard API called: /employee-directory");

        List<ProcedureEmployeeDirectoryDto> payload = adminDashboardService.getEmployeeDirectoryFromProcedure();
        return ResponseEntity.ok(new ApiResponse(true, "Employee directory fetched successfully", payload));
    }

    @GetMapping("/on-leave")
    public ResponseEntity<ApiResponse> getOnLeaveEmployees(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /on-leave with date={}", date);

        return okEmployeeCard("On-leave employees fetched successfully", adminDashboardService.getOnLeave(date));
    }

    @GetMapping("/on-leave/date={date}")
    public ResponseEntity<ApiResponse> getOnLeaveEmployeesByPath(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /on-leave/date={} (path style)", date);

        return okEmployeeCard("On-leave employees fetched successfully", adminDashboardService.getOnLeave(date));
    }

    @GetMapping("/unknown-alerts")
    public ResponseEntity<ApiResponse> getUnknownAlerts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /unknown-alerts with date={}", date);

        return okUnknownAlerts("Unknown alerts fetched successfully", adminDashboardService.getUnknownAlerts(date));
    }

    @GetMapping("/unknown-alerts/date={date}")
    public ResponseEntity<ApiResponse> getUnknownAlertsByPath(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Dashboard API called: /unknown-alerts/date={} (path style)", date);

        return okUnknownAlerts("Unknown alerts fetched successfully", adminDashboardService.getUnknownAlerts(date));
    }

    private ResponseEntity<ApiResponse> okAttendance(String message, AttendanceCardResponseDto payload) {
        log.info("Dashboard response: message='{}', date={}, count={}", message, payload.getDate(), payload.getTotalCount());
        return ResponseEntity.ok(new ApiResponse(true, message, payload));
    }

    private ResponseEntity<ApiResponse> okEmployeeCard(String message, EmployeeCardResponseDto payload) {
        log.info("Dashboard response: message='{}', date={}, count={}", message, payload.getDate(), payload.getTotalCount());
        return ResponseEntity.ok(new ApiResponse(true, message, payload));
    }

    private ResponseEntity<ApiResponse> okUnknownAlerts(String message, UnknownAlertsResponseDto payload) {
        log.info("Dashboard response: message='{}', date={}, count={}", message, payload.getDate(), payload.getTotalCount());
        return ResponseEntity.ok(new ApiResponse(true, message, payload));
    }
}
