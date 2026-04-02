package com.stations.facedetection.User.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stations.facedetection.User.Entity.RoleEntity;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
	 Optional<RoleEntity> findByName(String name);
}
