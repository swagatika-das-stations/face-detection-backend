package com.stations.facedetection.Auth.Service;

import com.stations.facedetection.Auth.DTO.LoginRequestDto;
import com.stations.facedetection.Auth.DTO.LoginResponseDto;
import com.stations.facedetection.Auth.DTO.RegisterRequestDto;

public interface IAuthService {

	LoginResponseDto login(LoginRequestDto request); 
}
