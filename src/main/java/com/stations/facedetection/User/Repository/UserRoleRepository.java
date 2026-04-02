package com.stations.facedetection.User.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stations.facedetection.User.Entity.UserRoleEntity;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long>{

}
