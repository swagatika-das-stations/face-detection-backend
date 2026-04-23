package com.stations.facedetection.Dashboard.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeInfoDto {

    private Long id;
    private String employeeId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String profileImage; // Base64-encoded first face image
}
