package com.stations.facedetection.Dashboard.Service;

import java.time.LocalDate;
import java.time.LocalTime;
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

        log.info("Fetching check-in data. Requested date={}", date);

        LocalDate resolvedDate = resolveAttendanceDate(date);

        log.info("Resolved check-in date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findCheckinsByDateOrderByTimestampAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        log.info("Check-in records fetched successfully. date={}, count={}", resolvedDate, records.size());

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getCheckouts(LocalDate date) {

        log.info("Fetching check-out data. Requested date={}", date);

        LocalDate resolvedDate = resolveAttendanceDate(date);

        log.info("Resolved checkout date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findCheckoutsByDateOrderByTimestampDesc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        log.info("Check-out records fetched successfully. date={}, count={}", resolvedDate, records.size());

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getHeadcount(LocalDate date) {

        log.info("Fetching headcount data. Requested date={}", date);

        LocalDate resolvedDate = resolveAttendanceDate(date);

        log.info("Resolved headcount date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findHeadcountByDate(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        log.info("Headcount records fetched successfully. date={}, count={}", resolvedDate, records.size());

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public EmployeeCardResponseDto getTotalEmployees() {

        log.info("Fetching total employee list");

        List<EmployeeInfoDto> employees = employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(this::buildFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toEmployeeInfoDto)
                .toList();

        log.info("Total employees fetched successfully. count={}", employees.size());

        return new EmployeeCardResponseDto(null, employees.size(), employees);
    }

    public EmployeeCardResponseDto getOnLeave(LocalDate date) {

        LocalDate resolvedDate = resolveDate(date);

        log.info("Fetching on-leave employees for date={}", resolvedDate);

        Set<String> checkedInNames = employeeCheckinCheckoutRepository
                .findCheckinsByDateOrderByTimestampAsc(resolvedDate)
                .stream()
                .map(EmployeeCheckinCheckoutEntity::getName)
                .map(this::normalizeName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<EmployeeInfoDto> onLeaveEmployees = employeeRepository.findAll().stream()
                .filter(emp -> !checkedInNames.contains(normalizeName(buildFullName(emp))))
                .sorted(Comparator.comparing(this::buildFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toEmployeeInfoDto)
                .toList();

        log.info("On-leave employees calculated. date={}, count={}", resolvedDate, onLeaveEmployees.size());

        return new EmployeeCardResponseDto(resolvedDate, onLeaveEmployees.size(), onLeaveEmployees);
    }

    public UnknownAlertsResponseDto getUnknownAlerts(LocalDate date) {

        log.info("Fetching unknown alerts. Requested date={}", date);

        LocalDate resolvedDate = resolveAttendanceDate(date);

        log.info("Resolved unknown alert date={}", resolvedDate);

        Set<String> knownEmployees = employeeRepository.findAll().stream()
                .map(this::buildFullName)
                .map(this::normalizeName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, UnknownAlertDto> unknownByName = new LinkedHashMap<>();

        employeeCheckinCheckoutRepository.findByDateOrderByNameAsc(resolvedDate)
                .forEach(entity -> {

                    String normalizedName = normalizeName(entity.getName());

                    if (knownEmployees.contains(normalizedName)
                            || unknownByName.containsKey(normalizedName)) {
                        return;
                    }

                    UnknownAlertDto alert = new UnknownAlertDto(
                            entity.getTimestamp().toLocalDate(),
                            entity.getName(),
                            entity.getTimestamp().toLocalTime(),
                            entity.getLocationName());

                    unknownByName.put(normalizedName, alert);
                });

        List<UnknownAlertDto> unknownPersons = unknownByName.values().stream().toList();

        log.info("Unknown alerts fetched successfully. date={}, count={}", resolvedDate, unknownPersons.size());

        return new UnknownAlertsResponseDto(resolvedDate, unknownPersons.size(), unknownPersons);
    }

    private LocalDate resolveDate(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }

    private LocalDate resolveAttendanceDate(LocalDate requestedDate) {

        LocalDate resolvedDate = resolveDate(requestedDate);

        boolean hasRows = !employeeCheckinCheckoutRepository
                .findByDateOrderByNameAsc(resolvedDate)
                .isEmpty();

        if (hasRows) {
            return resolvedDate;
        }

        log.warn("No attendance rows found for date={}, fetching latest available date", resolvedDate);

        return employeeCheckinCheckoutRepository
                .findTopByOrderByTimestampDesc()
                .map(entity -> entity.getTimestamp().toLocalDate())
                .orElse(resolvedDate);
    }

    private CheckinCheckoutRecordDto toRecordDto(EmployeeCheckinCheckoutEntity entity) {

        LocalTime time = entity.getTimestamp().toLocalTime();
        String status = entity.getDirection().equals("entry") ? "CHECKED_IN" : "CHECKED_OUT";

        return new CheckinCheckoutRecordDto(
                entity.getTimestamp().toLocalDate(),
                entity.getName(),
                entity.getDirection().equals("entry") ? time : null,
                entity.getDirection().equals("exit") ? time : null,
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
        if (name == null) return "";
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
