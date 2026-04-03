package com.stations.facedetection.integration.kloudspot.DTO;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Kloudspot identity search API
 * GET /advanced/api/v1/humans/get?identity={identity}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponseDto {
    private String emailAddress;
    private Map<String, Object> fingerprintInfo;
    private String firstName;
    private String fullName;
    private String identity;
    private String lastName;
    private Map<String, Object> meta;
    private String registrationStatus;  // ALLOWED, OPENFORREGISTRATION, NEEDSAPPROVAL, REREGISTER, ACTIVE, INACTIVE, PENDING
    private List<String> tags;
    private String uniqueId;
    private String userId;
    private Integer version;
}
