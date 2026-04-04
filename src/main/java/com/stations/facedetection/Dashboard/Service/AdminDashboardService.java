package com.stations.facedetection.Dashboard.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.stations.facedetection.Dashboard.DTO.AttendanceCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.CheckinCheckoutRecordDto;
import com.stations.facedetection.Dashboard.DTO.EmployeeCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.EmployeeInfoDto;
import com.stations.facedetection.Dashboard.DTO.UnknownAlertDto;
import com.stations.facedetection.Dashboard.DTO.UnknownAlertsResponseDto;
import com.stations.facedetection.Dashboard.Entity.EmployeeCheckinCheckoutEntity;
import com.stations.facedetection.Dashboard.Repository.EmployeeCheckinCheckoutRepository;
import com.stations.facedetection.User.Entity.EmployeeEntity;
import com.stations.facedetection.User.Repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final EmployeeCheckinCheckoutRepository employeeCheckinCheckoutRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceCardResponseDto getCheckins(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);
        log.info("Fetching check-in records for date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNotNullOrderByFirstEntryTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        log.info("Check-in records fetched: date={}, count={}", resolvedDate, records.size());

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getCheckouts(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);
        log.info("Fetching check-out records for date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndLastExitTimeIsNotNullOrderByLastExitTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        log.info("Check-out records fetched: date={}, count={}", resolvedDate, records.size());

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getHeadcount(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);
        log.info("Fetching headcount records for date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNotNullAndLastExitTimeIsNullOrderByFirstEntryTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        log.info("Headcount records fetched: date={}, count={}", resolvedDate, records.size());

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public EmployeeCardResponseDto getTotalEmployees() {
        log.info("Fetching all employees for total-employees card");

        List<EmployeeInfoDto> employees = employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(this::buildFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toEmployeeInfoDto)
                .toList();

        log.info("Total employees fetched: count={}", employees.size());

        return new EmployeeCardResponseDto(null, employees.size(), employees);
    }

    public EmployeeCardResponseDto getOnLeave(LocalDate date) {

        LocalDate resolvedDate = resolveDate(date);
        log.info("Fetching on-leave employees for date={}", resolvedDate);

        List<EmployeeCheckinCheckoutEntity> leaveRecords =
            employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNullAndLastExitTimeIsNull(resolvedDate);

        List<EmployeeInfoDto> onLeaveEmployees = leaveRecords.stream()
            .map(record -> {
                EmployeeInfoDto dto = new EmployeeInfoDto();
                dto.setFullName(record.getName());
                dto.setFirstName(record.getName());
                dto.setLastName(record.getLocationName());
                return dto;
            })
            .sorted(Comparator.comparing(dto -> safeTrim(dto.getFullName()), String.CASE_INSENSITIVE_ORDER))
            .toList();

        log.info("On-leave employees fetched: date={}, totalOnLeave={}",
            resolvedDate,
            onLeaveEmployees.size());

        return new EmployeeCardResponseDto(resolvedDate, onLeaveEmployees.size(), onLeaveEmployees);
    }

    public UnknownAlertsResponseDto getUnknownAlerts(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);
        log.info("Fetching unknown alerts for date={}", resolvedDate);

        Set<String> knownEmployees = employeeRepository.findAll().stream()
                .map(this::buildFullName)
                .map(this::normalizeName)
                .filter(normalized -> !normalized.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, UnknownAlertDto> unknownByName = new LinkedHashMap<>();

        employeeCheckinCheckoutRepository.findByDateOrderByNameAsc(resolvedDate)
                .forEach(entity -> {
                    String normalizedName = normalizeName(entity.getName());
                    if (normalizedName.isBlank() || knownEmployees.contains(normalizedName)
                            || unknownByName.containsKey(normalizedName)) {
                        return;
                    }

                    UnknownAlertDto alert = new UnknownAlertDto(
                            entity.getDate(),
                            entity.getName(),
                            entity.getFirstEntryTime(),
                            entity.getLocationName());

                    unknownByName.put(normalizedName, alert);
                });

        List<UnknownAlertDto> unknownPersons = unknownByName.values().stream().toList();
        log.info("Unknown alerts fetched: date={}, knownEmployees={}, unknownAlerts={}",
                resolvedDate,
                knownEmployees.size(),
                unknownPersons.size());

        return new UnknownAlertsResponseDto(resolvedDate, unknownPersons.size(), unknownPersons);
    }

    private LocalDate resolveDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
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

    private EmployeeInfoDto toEmployeeInfoDto(EmployeeEntity entity) {
        return new EmployeeInfoDto(
                entity.getId(),
                entity.getEmployeeId(),
                buildFullName(entity),
                safeTrim(entity.getFirstName()),
                safeTrim(entity.getLastName()),
                entity.getUser() == null ? null : entity.getUser().getEmail());
    }

    private String buildFullName(EmployeeEntity employee) {
        return (safeTrim(employee.getFirstName()) + " " + safeTrim(employee.getLastName())).trim();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
