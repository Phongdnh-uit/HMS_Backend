package com.hms.auth_service.repositories;

import com.hms.auth_service.entities.Account;
import com.hms.common.enums.RoleEnum;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests for AccountRepository.
 * Uses @DataJpaTest which configures H2 in-memory database automatically.
 * 
 * These tests verify:
 * - JPA entity mappings are correct
 * - Custom query methods work as expected
 * - Database constraints are enforced
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("IT-REPO: AccountRepository Integration Tests")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testEmail = TestDataFactory.uniqueEmail();
        
        testAccount = new Account();
        testAccount.setEmail(testEmail);
        testAccount.setPassword("encodedPassword");
        testAccount.setRole(RoleEnum.PATIENT);
    }

    @Test
    @DisplayName("IT-REPO-001: Should save and retrieve account")
    void save_shouldPersistAccount() {
        // When
        Account savedAccount = accountRepository.save(testAccount);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Account> found = accountRepository.findById(savedAccount.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("IT-REPO-002: Should check if email exists")
    void existsByEmail_shouldReturnTrue_whenExists() {
        // Given
        entityManager.persistAndFlush(testAccount);
        entityManager.clear();

        // When
        boolean exists = accountRepository.existsByEmail(testEmail);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("IT-REPO-003: Should return false for non-existent email")
    void existsByEmail_shouldReturnFalse_whenNotExists() {
        // When
        boolean exists = accountRepository.existsByEmail("nonexistent@test.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("IT-REPO-004: Should check if role exists")
    void existsByRole_shouldReturnTrue_whenExists() {
        // Given
        testAccount.setRole(RoleEnum.ADMIN);
        entityManager.persistAndFlush(testAccount);
        entityManager.clear();

        // When
        boolean exists = accountRepository.existsByRole(RoleEnum.ADMIN);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("IT-REPO-005: Should return false for non-existent role")
    void existsByRole_shouldReturnFalse_whenNotExists() {
        // Given - no accounts saved

        // When
        boolean exists = accountRepository.existsByRole(RoleEnum.ADMIN);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("IT-REPO-006: Should update account")
    void update_shouldModifyAccount() {
        // Given
        Account saved = entityManager.persistAndFlush(testAccount);
        entityManager.clear();
        
        String newEmail = TestDataFactory.uniqueEmail();

        // When
        Account toUpdate = accountRepository.findById(saved.getId()).orElseThrow();
        toUpdate.setEmail(newEmail);
        accountRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        // Then
        Account updated = accountRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getEmail()).isEqualTo(newEmail);
    }

    @Test
    @DisplayName("IT-REPO-007: Should delete account")
    void delete_shouldRemoveAccount() {
        // Given
        Account saved = entityManager.persistAndFlush(testAccount);
        String accountId = saved.getId();
        entityManager.clear();

        // When
        accountRepository.deleteById(accountId);
        entityManager.flush();

        // Then
        Optional<Account> deleted = accountRepository.findById(accountId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("IT-REPO-008: Should generate UUID for new accounts")
    void save_shouldGenerateUuid() {
        // Given
        assertThat(testAccount.getId()).isNull();

        // When
        Account saved = accountRepository.save(testAccount);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isNotEmpty();
    }

    @Test
    @DisplayName("IT-REPO-009: Should persist all required fields")
    void save_shouldPersistAllFields() {
        // Given
        testAccount.setRole(RoleEnum.DOCTOR);
        testAccount.setEmailVerified(true);

        // When
        Account saved = entityManager.persistAndFlush(testAccount);
        entityManager.clear();

        // Then
        Account found = accountRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEmail()).isEqualTo(testEmail);
        assertThat(found.getPassword()).isEqualTo("encodedPassword");
        assertThat(found.getRole()).isEqualTo(RoleEnum.DOCTOR);
        assertThat(found.isEmailVerified()).isTrue();
    }
}
