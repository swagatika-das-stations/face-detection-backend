package com.stations.facedetection.Dashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureEmployeeDirectoryDto {

    private String employeeId;
    private String email;
    private String name;
    private String date;
    private String locationName;
    private String firstEntryTime;
    private String lastExitTime;
}
