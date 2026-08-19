package com.ecommerce.monolith.user.config;

import com.ecommerce.monolith.user.entity.User;
import com.ecommerce.monolith.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:Administrator}")
    private String adminName;

    @Override
    @Transactional
    public void run(String... args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.info("Admin bootstrap skipped. Set ADMIN_EMAIL and ADMIN_PASSWORD to create an admin user.");
            return;
        }

        User admin = userRepository.findByEmail(adminEmail)
                .map(existingUser -> {
                    existingUser.setPassword(passwordEncoder.encode(adminPassword));
                    existingUser.setName(adminName);
                    existingUser.setRole(User.UserRole.ADMIN);
                    existingUser.setStatus(User.UserStatus.ACTIVE);
                    return existingUser;
                })
                .orElseGet(() -> User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .name(adminName)
                        .role(User.UserRole.ADMIN)
                        .status(User.UserStatus.ACTIVE)
                        .build());

        userRepository.save(admin);
        log.info("Admin user bootstrapped: {}", adminEmail);
    }
}
