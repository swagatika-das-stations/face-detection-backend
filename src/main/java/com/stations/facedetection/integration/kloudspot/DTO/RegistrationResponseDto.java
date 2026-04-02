package com.stations.facedetection.integration.kloudspot.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationResponseDto {
    private String STATUS;
    private String entityId;
    private String message;

}
