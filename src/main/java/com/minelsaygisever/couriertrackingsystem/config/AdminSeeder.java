package com.minelsaygisever.couriertrackingsystem.config;

import com.minelsaygisever.couriertrackingsystem.domain.AdminProfile;
import com.minelsaygisever.couriertrackingsystem.domain.Role;
import com.minelsaygisever.couriertrackingsystem.domain.User;
import com.minelsaygisever.couriertrackingsystem.repository.AdminProfileRepository;
import com.minelsaygisever.couriertrackingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            log.info("Default admin user not found, creating one...");

            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .isEnabled(true)
                    .build();

            AdminProfile adminProfile = AdminProfile.builder()
                    .user(adminUser)
                    .fullName("Default Admin")
                    .build();

            adminProfileRepository.save(adminProfile);

            log.info("Default admin user created. Username: 'admin', Password: 'admin123'");
        } else {
            log.info("Default admin user already exists. Skipping creation.");
        }
    }
}
