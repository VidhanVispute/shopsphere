package com.shopsphere.auth.security;

import com.shopsphere.auth.entity.User;
import com.shopsphere.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        // Idempotent — safe to run on every startup
        if (userRepository.existsByEmail("admin@shopsphere.com")) {
            log.info("Admin user already exists — skipping seed");
            return;
        }

        User admin = User.builder()
                .email("admin@shopsphere.com")
                .password(passwordEncoder.encode("Admin@ShopSphere#2026"))
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.info("Admin user seeded successfully");
    }
}