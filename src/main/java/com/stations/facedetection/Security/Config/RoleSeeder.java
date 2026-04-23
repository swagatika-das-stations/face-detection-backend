package com.stations.facedetection.Security.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.stations.facedetection.User.Entity.RoleEntity;
import com.stations.facedetection.User.Repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRole("ADMIN");
        seedRole("EMPLOYEE");
        log.info("Role seeding completed");
    }

    private void seedRole(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            RoleEntity role = new RoleEntity();
            role.setName(roleName);
            roleRepository.save(role);
            log.info("Seeded role: {}", roleName);
        } else {
            log.debug("Role already exists: {}", roleName);
        }
    }
}
