package com.stations.facedetection.User.Entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name="users")
@Entity
public class UserEntity {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(unique = true, nullable = false)
	    private String email;

	    @Column(nullable = false)
	    private String password;

	    private boolean enabled = true;

	    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<UserRoleEntity> userRoles;
	    
	    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
	    private EmployeeEntity employee;
	    
	    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	    private List<FaceRegistryEntity> faceRegistries;

}

