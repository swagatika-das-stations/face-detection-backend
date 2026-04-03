package com.stations.facedetection.Dashboard.Entity;

import java.time.LocalDate;
import java.time.LocalTime;

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

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "first_entry_time")
    private LocalTime firstEntryTime;

    @Column(name = "last_exit_time")
    private LocalTime lastExitTime;

    @Column(name = "location_name", length = 50)
    private String locationName;
}
