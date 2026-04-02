package com.stations.facedetection.User.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stations.facedetection.User.Entity.EmployeeEntity;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

}
