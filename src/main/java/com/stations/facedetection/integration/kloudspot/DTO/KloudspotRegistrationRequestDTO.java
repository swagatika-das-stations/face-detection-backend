package com.stations.facedetection.integration.kloudspot.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for human.json file to be included in Kloudspot ZIP request
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KloudspotRegistrationRequestDTO {

    @JsonProperty("identity")
    private String identity;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("emailAddress")
    private String emailAddress;
    
    @JsonProperty("meta")
    private Meta meta;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta {
        @JsonProperty("employeeId")
        private String employeeId;
    }
}