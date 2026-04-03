package com.stations.facedetection.Dashboard.DTO;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnknownAlertsResponseDto {

    private LocalDate date;
    private long totalCount;
    private List<UnknownAlertDto> persons;
}
