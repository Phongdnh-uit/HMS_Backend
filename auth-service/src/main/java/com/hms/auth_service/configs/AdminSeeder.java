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
 * Account seeder that creates default accounts for all roles on application startup.
 * Only creates accounts if they don't already exist.
 * 
 * Default credentials (password format: {Role}123!@):
 * - ADMIN:        admin@hms.com       / Admin123!@
 * - DOCTOR:       doctor@hms.com      / Doctor123!@
 * - NURSE:        nurse@hms.com       / Nurse123!@
 * - RECEPTIONIST: receptionist@hms.com / Receptionist123!@
 * - PATIENT:      patient@hms.com     / Patient123!@
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AdminSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    // Seed account configurations: email, password, role
    private static final Object[][] SEED_ACCOUNTS = {
        {"admin@hms.com", "Admin123!@", RoleEnum.ADMIN},
        {"doctor@hms.com", "Doctor123!@", RoleEnum.DOCTOR},
        {"nurse@hms.com", "Nurse123!@", RoleEnum.NURSE},
        {"receptionist@hms.com", "Receptionist123!@", RoleEnum.RECEPTIONIST},
        {"patient@hms.com", "Patient123!@", RoleEnum.PATIENT}
    };

    @Override
    public void run(String... args) {
        for (Object[] accountData : SEED_ACCOUNTS) {
            String email = (String) accountData[0];
            String password = (String) accountData[1];
            RoleEnum role = (RoleEnum) accountData[2];
            
            createAccountIfNotExists(email, password, role);
        }
    }

    private void createAccountIfNotExists(String email, String password, RoleEnum role) {
        if (accountRepository.existsByEmail(email)) {
            log.debug("Account already exists: {} ({})", email, role);
            return;
        }

        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setRole(role);
        account.setEmailVerified(true);

        accountRepository.save(account);
        log.info("Created seed account: {} with role {}", email, role);
    }
}
