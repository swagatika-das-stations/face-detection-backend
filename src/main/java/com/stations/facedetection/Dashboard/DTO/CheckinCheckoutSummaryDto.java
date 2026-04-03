package com.stations.facedetection.Dashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinCheckoutSummaryDto {

    private long totalDays;
    private long completedDays;
    private long openDays;
}
