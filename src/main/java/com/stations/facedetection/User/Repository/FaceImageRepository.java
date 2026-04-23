package com.stations.facedetection.User.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stations.facedetection.User.Entity.FaceImageEntity;

public interface FaceImageRepository extends JpaRepository<FaceImageEntity, Long> {

    @Query("SELECT f FROM FaceImageEntity f WHERE f.employee.id = :employeeId ORDER BY f.id ASC LIMIT 1")
    List<FaceImageEntity> findFirstByEmployeeId(@Param("employeeId") Long employeeId);

}
