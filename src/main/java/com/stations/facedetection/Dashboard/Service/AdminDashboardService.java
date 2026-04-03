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

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final EmployeeCheckinCheckoutRepository employeeCheckinCheckoutRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceCardResponseDto getCheckins(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNotNullOrderByFirstEntryTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getCheckouts(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndLastExitTimeIsNotNullOrderByLastExitTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getHeadcount(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNotNullAndLastExitTimeIsNullOrderByFirstEntryTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public EmployeeCardResponseDto getTotalEmployees() {
        List<EmployeeInfoDto> employees = employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(this::buildFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toEmployeeInfoDto)
                .toList();

        return new EmployeeCardResponseDto(null, employees.size(), employees);
    }

    public EmployeeCardResponseDto getOnLeave(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);

        List<EmployeeEntity> employeeEntities = employeeRepository.findAll();

        Set<String> checkedInNames = employeeCheckinCheckoutRepository.findByDateAndFirstEntryTimeIsNotNullOrderByFirstEntryTimeAsc(
                resolvedDate)
                .stream()
                .map(EmployeeCheckinCheckoutEntity::getName)
                .map(this::normalizeName)
                .filter(normalized -> !normalized.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<EmployeeInfoDto> onLeaveEmployees = employeeEntities.stream()
                .filter(employee -> !checkedInNames.contains(normalizeName(buildFullName(employee))))
                .sorted(Comparator.comparing(this::buildFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toEmployeeInfoDto)
                .toList();

        return new EmployeeCardResponseDto(resolvedDate, onLeaveEmployees.size(), onLeaveEmployees);
    }

    public UnknownAlertsResponseDto getUnknownAlerts(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);

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
