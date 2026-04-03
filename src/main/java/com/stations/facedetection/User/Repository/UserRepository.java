package com.stations.facedetection.User.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stations.facedetection.User.Entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long>{
	Optional<UserEntity> findByEmail(String email);

	@Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.roles WHERE u.email = :email")
	Optional<UserEntity> findByEmailWithRoles(@Param("email") String email);
}
