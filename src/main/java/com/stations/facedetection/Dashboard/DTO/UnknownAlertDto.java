package com.stations.facedetection.Dashboard.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnknownAlertDto {

    private LocalDate date;
    private String name;
    private LocalTime firstEntryTime;
    private String locationName;
}
