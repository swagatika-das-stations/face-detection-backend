package com.stations.facedetection.Dashboard.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "employee_checkin_checkout")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeCheckinCheckoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false, length = 20)
    private String locationType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String locationName;

    @Column(nullable = false, length = 100)
    private String locationFullName;
}
