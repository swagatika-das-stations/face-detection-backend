package com.stations.facedetection.Dashboard.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stations.facedetection.Dashboard.Entity.EmployeeCheckinCheckoutEntity;

public interface EmployeeCheckinCheckoutRepository extends JpaRepository<EmployeeCheckinCheckoutEntity, Long> {

    List<EmployeeCheckinCheckoutEntity> findByNameIgnoreCaseAndDateBetweenOrderByDateDesc(
            String name,
            LocalDate startDate,
            LocalDate endDate);

    List<EmployeeCheckinCheckoutEntity> findByDateOrderByNameAsc(LocalDate date);

    List<EmployeeCheckinCheckoutEntity> findByDateAndFirstEntryTimeIsNotNullOrderByFirstEntryTimeAsc(LocalDate date);

    List<EmployeeCheckinCheckoutEntity> findByDateAndLastExitTimeIsNotNullOrderByLastExitTimeAsc(LocalDate date);

    List<EmployeeCheckinCheckoutEntity> findByDateAndFirstEntryTimeIsNotNullAndLastExitTimeIsNullOrderByFirstEntryTimeAsc(
            LocalDate date);
}
