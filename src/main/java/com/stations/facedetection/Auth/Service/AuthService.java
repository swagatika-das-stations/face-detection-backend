package com.stations.facedetection.Auth.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stations.facedetection.Auth.DTO.LoginRequestDto;
import com.stations.facedetection.Auth.DTO.LoginResponseDto;
import com.stations.facedetection.Auth.DTO.RegisterRequestDto;
import com.stations.facedetection.Security.Jwt.JwtUtil;
import com.stations.facedetection.User.Entity.EmployeeEntity;
import com.stations.facedetection.User.Entity.FaceImageEntity;
import com.stations.facedetection.User.Entity.RoleEntity;
import com.stations.facedetection.User.Entity.UserEntity;
import com.stations.facedetection.User.Entity.UserRoleEntity;
import com.stations.facedetection.User.Repository.EmployeeRepository;
import com.stations.facedetection.User.Repository.FaceImageRepository;
import com.stations.facedetection.User.Repository.RoleRepository;
import com.stations.facedetection.User.Repository.UserRepository;
import com.stations.facedetection.User.Repository.UserRoleRepository;
import com.stations.facedetection.common.exception.PasswordMismatchException;
import com.stations.facedetection.common.exception.ResourceNotFoundException;
import com.stations.facedetection.common.exception.UserAlreadyExistsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

   //Autowired classes....
    private final UserRepository UserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmployeeRepository employeeRepository;
    private final FaceImageRepository faceImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    //..............................Register.....................................
	/*
	 * @Override public void register(RegisterRequestDto request) throws
	 * RuntimeException {
	 * 
	 * 
	 * // 1️. Check if email already exists if
	 * (UserRepository.findByEmail(request.getEmail()).isPresent()) { throw new
	 * UserAlreadyExistsException("Email already registered"); }
	 * 
	 * // 2️. Validate password & confirm password if
	 * (!request.getPassword().equals(request.getConfirmPassword())) { throw new
	 * PasswordMismatchException("Passwords do not match"); }
	 * 
	 * // 3️. Create User (AUTH DATA) UserEntity user = new UserEntity();
	 * user.setEmail(request.getEmail());
	 * user.setPassword(passwordEncoder.encode(request.getPassword()));
	 * user.setEnabled(true);
	 * 
	 * UserEntity savedUser = UserRepository.save(user);
	 * 
	 * // 4️. Assign EMPLOYEE Role RoleEntity role =
	 * roleRepository.findByName("EMPLOYEE") .orElseThrow(() -> new
	 * ResourceNotFoundException("EMPLOYEE role not found"));
	 * 
	 * UserRoleEntity userRole = new UserRoleEntity(); userRole.setUsers(savedUser);
	 * userRole.setRoles(role);
	 * 
	 * userRoleRepository.save(userRole);
	 * 
	 * // 5️. Create Employee (BUSINESS DATA) EmployeeEntity employee = new
	 * EmployeeEntity(); employee.setFirstName(request.getFirstName());
	 * employee.setLastName(request.getLastName());
	 * employee.setEmployeeId(request.getEmployeeId()); employee.setUser(savedUser);
	 * 
	 * EmployeeEntity savedEmployee = employeeRepository.save(employee);
	 * 
	 * // 6️. Save Face Images (if available) if (request.getFaceImages() != null &&
	 * !request.getFaceImages().isEmpty()) {
	 * 
	 * List<FaceImageEntity> images = new ArrayList<>();
	 * 
	 * for (String img : request.getFaceImages()) { FaceImageEntity faceImage = new
	 * FaceImageEntity(); faceImage.setImagePath(img);
	 * faceImage.setEmployee(savedEmployee); images.add(faceImage); }
	 * 
	 * faceImageRepository.saveAll(images); }
	 * 
	 * }
	 */
	
	//..............................Login.........................................
	
	@Override
	public LoginResponseDto login(LoginRequestDto request) {

	    Authentication authentication = authenticationManager.authenticate(
	            new UsernamePasswordAuthenticationToken(
	                    request.getEmail(),
	                    request.getPassword()
	            )
	    );

	    UserEntity user = UserRepository.findByEmailWithRoles(request.getEmail())
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    String token = jwtUtil.generateToken(user.getEmail());

	    List<String> roles = user.getUserRoles().stream()
	            .map(userRole -> userRole.getRoles().getName())
	            .filter(roleName -> roleName != null && !roleName.trim().isEmpty())
	            .map(String::trim)
	            .distinct()
	            .collect(Collectors.toList());

	    String primaryRole = resolvePrimaryRole(roles);
	    boolean multipleRoles = roles.size() > 1;
	    String dashboardPath = resolveDashboardPath(primaryRole, multipleRoles);

	    return new LoginResponseDto(token, user.getId(), primaryRole, roles, multipleRoles, dashboardPath);
	}

	private String resolveDashboardPath(String primaryRole, boolean multipleRoles) {
	    if (multipleRoles || primaryRole == null) {
	        return null;
	    }

	    String normalizedRole = primaryRole.trim().toUpperCase();
	    return switch (normalizedRole) {
	        case "ADMIN" -> "/admin/dashboard";
	        case "EMPLOYEE" -> "/employee/dashboard";
	        default -> null;
	    };
	}

	private String resolvePrimaryRole(List<String> roles) {
	    if (roles == null || roles.isEmpty()) {
	        return null;
	    }

	    return roles.stream()
	            .map(role -> role == null ? null : role.trim().toUpperCase())
	            .filter(role -> role != null && !role.isBlank())
	            .filter(role -> role.equals("ADMIN"))
	            .findFirst()
	            .orElse(roles.get(0));
	}
}
