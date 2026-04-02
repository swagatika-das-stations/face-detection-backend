package com.stations.facedetection.User.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stations.facedetection.User.Entity.FaceRegistryEntity;

public interface FaceRegistryRepository extends JpaRepository<FaceRegistryEntity, Long> {


}
