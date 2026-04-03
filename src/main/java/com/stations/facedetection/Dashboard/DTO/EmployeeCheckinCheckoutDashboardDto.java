package com.stations.facedetection.Dashboard.DTO;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCheckinCheckoutDashboardDto {

    private String employeeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private CheckinCheckoutSummaryDto summary;
    private List<CheckinCheckoutRecordDto> records;
}
