package com.stations.facedetection.integration.kloudspot.DTO;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KloudspotRegistrationRequestDTO {

	@JsonProperty("human")
    private Human human;
	@JsonProperty("zipFile")
    private String zipFile;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Human {
    	
    	@JsonProperty("emailId")
        private String emailId;
        @JsonProperty("firstName")
        private String firstName;
        @JsonProperty("identity")
        private String identity;
        @JsonProperty("lastName")
        private String lastName;
        @JsonProperty("meta")
        private Meta meta;
        @JsonProperty("tags")
        private List<String> tags;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta {
    	@JsonProperty("employeeid")
        private String employeeid;
    }
}