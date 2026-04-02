package com.stations.facedetection.User.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stations.facedetection.User.Entity.FaceImageEntity;

public interface FaceImageRepository extends JpaRepository<FaceImageEntity, Long> {

}
