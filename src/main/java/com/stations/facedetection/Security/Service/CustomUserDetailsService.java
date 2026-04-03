package com.stations.facedetection.Security.Service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.stations.facedetection.Security.Model.CustomUserDetails;
import com.stations.facedetection.User.Entity.UserEntity;
import com.stations.facedetection.User.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
	
    //Autowired classes...
    private final UserRepository userRepository;

    //check for existing user...
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                UserEntity user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user);
	}
	//

}
