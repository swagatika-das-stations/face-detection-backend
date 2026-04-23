package com.stations.facedetection.User.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stations.facedetection.User.Entity.EmployeeEntity;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

	Optional<EmployeeEntity> findByUserId(Long userId);

	Optional<EmployeeEntity> findByEmployeeId(String employeeId);

	List<EmployeeEntity> findByEntityIdIsNotNull();

}
