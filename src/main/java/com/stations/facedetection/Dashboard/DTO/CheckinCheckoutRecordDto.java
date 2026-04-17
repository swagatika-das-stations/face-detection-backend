package com.stations.facedetection.Dashboard.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinCheckoutRecordDto {

    private LocalDate date;
    private String name;
    private String employeeId;
    private String email;
    private LocalTime firstEntryTime;
    private LocalTime lastExitTime;
    private String locationName;
    private String status;
}
