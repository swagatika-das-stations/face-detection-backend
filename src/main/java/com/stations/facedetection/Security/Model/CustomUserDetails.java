package com.stations.facedetection.Security.Model;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.stations.facedetection.User.Entity.UserEntity;
import com.stations.facedetection.User.Entity.UserRoleEntity;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
	
	 private static final long serialVersionUID = 1L;

	    private final UserEntity user;

	    //Convert roles → authorities
	    @Override
	    public Collection<? extends GrantedAuthority> getAuthorities() {
	        List<UserRoleEntity> roles = user.getUserRoles();

	        return roles.stream()
	                .map(role -> new SimpleGrantedAuthority(
	                        "ROLE_" + role.getRoles().getName()
	                ))
	                .collect(Collectors.toList());
	    }
	    
	    // Username (we use email)
	    @Override
	    public String getUsername() {
	        return user.getEmail();
	    }

	    //Password
	    @Override
	    public String getPassword() {
	        return user.getPassword();
	    }

	    //Account status checks
	    @Override
	    public boolean isAccountNonExpired() {
	        return true;
	    }

	    @Override
	    public boolean isAccountNonLocked() {
	        return true;
	    }

	    @Override
	    public boolean isCredentialsNonExpired() {
	        return true;
	    }

	    @Override
	    public boolean isEnabled() {
	        return user.isEnabled();
	    }

	    //get original UserEntity
	    public UserEntity getUser() {
	        return user;
	    }

		
}
