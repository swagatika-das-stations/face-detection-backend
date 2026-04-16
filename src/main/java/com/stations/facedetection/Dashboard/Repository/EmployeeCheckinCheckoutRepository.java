package com.stations.facedetection.Dashboard.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stations.facedetection.Dashboard.Entity.EmployeeCheckinCheckoutEntity;

public interface EmployeeCheckinCheckoutRepository extends JpaRepository<EmployeeCheckinCheckoutEntity, Long> {

    Optional<EmployeeCheckinCheckoutEntity> findTopByOrderByTimestampDesc();

    List<EmployeeCheckinCheckoutEntity> findByNameIgnoreCaseAndTimestampBetweenOrderByTimestampDesc(
            String name,
            LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("SELECT e FROM EmployeeCheckinCheckoutEntity e WHERE DATE(e.timestamp) = :date ORDER BY e.name ASC")
    List<EmployeeCheckinCheckoutEntity> findByDateOrderByNameAsc(@Param("date") LocalDate date);

    @Query("SELECT e FROM EmployeeCheckinCheckoutEntity e WHERE DATE(e.timestamp) = :date AND e.direction = 'entry' ORDER BY e.timestamp ASC")
    List<EmployeeCheckinCheckoutEntity> findCheckinsByDateOrderByTimestampAsc(@Param("date") LocalDate date);

    @Query("SELECT e FROM EmployeeCheckinCheckoutEntity e WHERE DATE(e.timestamp) = :date AND e.direction = 'exit' ORDER BY e.timestamp DESC")
    List<EmployeeCheckinCheckoutEntity> findCheckoutsByDateOrderByTimestampDesc(@Param("date") LocalDate date);

    @Query("SELECT e FROM EmployeeCheckinCheckoutEntity e WHERE DATE(e.timestamp) = :date AND e.direction = 'entry' AND e.name NOT IN (SELECT e2.name FROM EmployeeCheckinCheckoutEntity e2 WHERE DATE(e2.timestamp) = :date AND e2.direction = 'exit') ORDER BY e.timestamp ASC")
    List<EmployeeCheckinCheckoutEntity> findHeadcountByDate(@Param("date") LocalDate date);

}
