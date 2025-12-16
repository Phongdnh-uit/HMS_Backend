package com.hms.auth_service.configs;

import com.hms.auth_service.entities.Account;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.enums.RoleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Admin seeder that creates the default admin account on application startup.
 * Only creates the admin if no admin account exists in the database.
 * 
 * Default credentials:
 * - Email: admin@hms.com
 * - Password: Admin123!@
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AdminSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin@hms.com";
    private static final String ADMIN_PASSWORD = "Admin123!@";

    @Override
    public void run(String... args) {
        // Check if admin already exists
        if (accountRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin account already exists: {}", ADMIN_EMAIL);
            return;
        }

        // Check if ANY admin exists
        if (accountRepository.existsByRole(RoleEnum.ADMIN)) {
            log.info("An admin account already exists in the system");
            return;
        }

        // Create default admin account
        Account admin = new Account();
        admin.setEmail(ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(RoleEnum.ADMIN);
        admin.setEmailVerified(true);

        accountRepository.save(admin);
        log.info("Created default admin account: {}", ADMIN_EMAIL);
    }
}
