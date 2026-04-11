package com.stations.facedetection.Dashboard.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.stations.facedetection.Dashboard.DTO.CheckinCheckoutRecordDto;
import com.stations.facedetection.Dashboard.DTO.CheckinCheckoutSummaryDto;
import com.stations.facedetection.Dashboard.DTO.EmployeeCheckinCheckoutDashboardDto;
import com.stations.facedetection.Dashboard.Entity.EmployeeCheckinCheckoutEntity;
import com.stations.facedetection.Dashboard.Repository.EmployeeCheckinCheckoutRepository;
import com.stations.facedetection.User.Entity.EmployeeEntity;
import com.stations.facedetection.User.Entity.UserEntity;
import com.stations.facedetection.User.Repository.UserRepository;
import com.stations.facedetection.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeCheckinCheckoutService {

    private final EmployeeCheckinCheckoutRepository employeeCheckinCheckoutRepository;
    private final UserRepository userRepository;

    public EmployeeCheckinCheckoutDashboardDto getDashboard(String email, LocalDate startDate, LocalDate endDate) {

        log.info("Employee dashboard requested for email={}, startDate={}, endDate={}", email, startDate, endDate);

        LocalDate resolvedEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate resolvedStartDate = startDate == null ? resolvedEndDate.minusDays(6) : startDate;

        log.info("Resolved date range: startDate={}, endDate={}", resolvedStartDate, resolvedEndDate);

        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            log.warn("Invalid date range: startDate={} is after endDate={}", resolvedStartDate, resolvedEndDate);
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found for email={}", email);
                    return new ResourceNotFoundException("User not found for email: " + email);
                });

        EmployeeEntity employee = user.getEmployee();
        if (employee == null) {
            log.error("Employee profile not found for user email={}", email);
            throw new ResourceNotFoundException("Employee profile not found for user: " + email);
        }

        String employeeName = buildEmployeeName(employee);

        log.info("Fetching attendance records for employeeName={}, dateRange={} to {}",
                employeeName, resolvedStartDate, resolvedEndDate);

        List<EmployeeCheckinCheckoutEntity> entities = employeeCheckinCheckoutRepository
                .findByNameIgnoreCaseAndDateBetweenOrderByDateDesc(employeeName, resolvedStartDate, resolvedEndDate);

        log.info("Attendance records fetched. employeeName={}, recordCount={}", employeeName, entities.size());

        List<CheckinCheckoutRecordDto> records = entities.stream()
                .map(this::toRecordDto)
                .toList();

        long totalDays = records.size();
        long completedDays = records.stream().filter(record -> record.getLastExitTime() != null).count();
        long openDays = totalDays - completedDays;

        log.info("Attendance summary calculated: totalDays={}, completedDays={}, openDays={}",
                totalDays, completedDays, openDays);

        CheckinCheckoutSummaryDto summary = new CheckinCheckoutSummaryDto(totalDays, completedDays, openDays);

        return new EmployeeCheckinCheckoutDashboardDto(
                employeeName,
                resolvedStartDate,
                resolvedEndDate,
                summary,
                records);
    }

    private CheckinCheckoutRecordDto toRecordDto(EmployeeCheckinCheckoutEntity entity) {

        String status = entity.getLastExitTime() == null ? "CHECKED_IN" : "CHECKED_OUT";

        return new CheckinCheckoutRecordDto(
                entity.getDate(),
                entity.getName(),
                entity.getFirstEntryTime(),
                entity.getLastExitTime(),
                entity.getLocationName(),
                status);
    }

    private String buildEmployeeName(EmployeeEntity employee) {

        String firstName = employee.getFirstName() == null ? "" : employee.getFirstName().trim();
        String lastName = employee.getLastName() == null ? "" : employee.getLastName().trim();

        String fullName = (firstName + " " + lastName).trim();

        if (fullName.isEmpty()) {
            log.error("Employee name missing for employeeId={}", employee.getId());
            throw new ResourceNotFoundException("Employee name is not available");
        }

        return fullName;
    }
}