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
import org.springframework.transaction.annotation.Transactional;

import com.stations.facedetection.Dashboard.DTO.AttendanceCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.CheckinCheckoutRecordDto;
import com.stations.facedetection.Dashboard.DTO.EmployeeCardResponseDto;
import com.stations.facedetection.Dashboard.DTO.EmployeeInfoDto;
import com.stations.facedetection.Dashboard.DTO.ProcedureEmployeeDirectoryDto;
import com.stations.facedetection.Dashboard.DTO.UnknownAlertDto;
import com.stations.facedetection.Dashboard.DTO.UnknownAlertsResponseDto;
import com.stations.facedetection.Dashboard.Entity.EmployeeCheckinCheckoutEntity;
import com.stations.facedetection.Dashboard.Repository.EmployeeCheckinCheckoutRepository;
import com.stations.facedetection.User.Entity.EmployeeEntity;
import com.stations.facedetection.User.Repository.EmployeeRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final EmployeeCheckinCheckoutRepository employeeCheckinCheckoutRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceSyncProcedureService attendanceSyncProcedureService;
    private final EntityManager entityManager;

    @Transactional
    public List<ProcedureEmployeeDirectoryDto> getEmployeeDirectoryFromProcedure() {

        attendanceSyncProcedureService.runSyncProcedureSafe();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
                .createNativeQuery(
                        "SELECT employee_id, email, name, date, location_name, first_entry_time, last_exit_time "
                                + "FROM employee_temp_main_checkincheckout ORDER BY employee_id")
                .getResultList();

        return rows.stream()
                .map(this::toProcedureDirectoryDto)
                .toList();
    }

    public AttendanceCardResponseDto getCheckins(LocalDate date) {

        attendanceSyncProcedureService.runSyncProcedureSafe();
        LocalDate resolvedDate = resolveAttendanceDate(date);

        log.info("Fetching check-in records for date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNotNullOrderByFirstEntryTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getCheckouts(LocalDate date) {

        attendanceSyncProcedureService.runSyncProcedureSafe();
        LocalDate resolvedDate = resolveAttendanceDate(date);

        log.info("Fetching check-out records for date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndLastExitTimeIsNotNullOrderByLastExitTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public AttendanceCardResponseDto getHeadcount(LocalDate date) {

        attendanceSyncProcedureService.runSyncProcedureSafe();
        LocalDate resolvedDate = resolveAttendanceDate(date);

        log.info("Fetching headcount records for date={}", resolvedDate);

        List<CheckinCheckoutRecordDto> records = employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNotNullAndLastExitTimeIsNullOrderByFirstEntryTimeAsc(resolvedDate)
                .stream()
                .map(this::toRecordDto)
                .toList();

        return new AttendanceCardResponseDto(resolvedDate, records.size(), records);
    }

    public EmployeeCardResponseDto getTotalEmployees() {

        attendanceSyncProcedureService.runSyncProcedureSafe();

        List<EmployeeInfoDto> employees = employeeRepository.findAll().stream()
                .sorted(Comparator.comparing(this::buildFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toEmployeeInfoDto)
                .toList();

        return new EmployeeCardResponseDto(null, employees.size(), employees);
    }

    public EmployeeCardResponseDto getOnLeave(LocalDate date) {

        LocalDate resolvedDate = resolveDate(date);

        Set<String> checkedInNames = employeeCheckinCheckoutRepository
                .findByDateAndFirstEntryTimeIsNotNullOrderByFirstEntryTimeAsc(resolvedDate)
                .stream()
                .map(EmployeeCheckinCheckoutEntity::getName)
                .map(this::normalizeName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<EmployeeInfoDto> onLeaveEmployees = employeeRepository.findAll().stream()
                .filter(emp -> !checkedInNames.contains(normalizeName(buildFullName(emp))))
                .sorted(Comparator.comparing(this::buildFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toEmployeeInfoDto)
                .toList();

        return new EmployeeCardResponseDto(resolvedDate, onLeaveEmployees.size(), onLeaveEmployees);
    }

    public UnknownAlertsResponseDto getUnknownAlerts(LocalDate date) {

        attendanceSyncProcedureService.runSyncProcedureSafe();
        LocalDate resolvedDate = resolveAttendanceDate(date);

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

    private LocalDate resolveAttendanceDate(LocalDate requestedDate) {

        LocalDate resolvedDate = resolveDate(requestedDate);

        boolean hasRows = !employeeCheckinCheckoutRepository
                .findByDateOrderByNameAsc(resolvedDate)
                .isEmpty();

        if (hasRows) {
            return resolvedDate;
        }

        return employeeCheckinCheckoutRepository
                .findTopByOrderByDateDesc()
                .map(EmployeeCheckinCheckoutEntity::getDate)
                .orElse(resolvedDate);
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
        if (name == null) return "";
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private ProcedureEmployeeDirectoryDto toProcedureDirectoryDto(Object[] row) {

        return new ProcedureEmployeeDirectoryDto(
                asText(row, 0),
                asText(row, 1),
                asText(row, 2),
                asText(row, 3),
                asText(row, 4),
                asText(row, 5),
                asText(row, 6));
    }

    private String asText(Object[] row, int index) {

        if (row == null || row.length <= index || row[index] == null) {
            return null;
        }

        return String.valueOf(row[index]);
    }
}