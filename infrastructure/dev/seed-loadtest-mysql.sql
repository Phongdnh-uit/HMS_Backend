-- ============================================================================
-- HMS Backend - Load Test Data Seeding Script (MySQL Version)
-- ============================================================================
-- Purpose: Seed test accounts for Gatling performance tests
-- 
-- Data Requirements:
--   - 1,000 patient accounts (patient1-1000@email.com)
--   - 60 doctor accounts (doctor1-60@hms.com)
--   - 50 nurse accounts (nurse1-50@hms.com)
--   - 40 receptionist accounts (receptionist1-40@hms.com)
--   - 5 admin accounts (admin1-5@hms.com)
--
-- Password: Password@123
-- BCrypt Hash: $2a$10$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2
-- ============================================================================

-- Use for auth-service database
USE auth_db;

-- Clear existing data (optional - comment out to keep existing data)
TRUNCATE TABLE accounts;

-- Create stored procedure for generating accounts
DELIMITER //

DROP PROCEDURE IF EXISTS seed_accounts//

CREATE PROCEDURE seed_accounts()
BEGIN
    DECLARE i INT DEFAULT 1;
    -- BCrypt hash for "Password@123" - generated from actual auth service registration
    DECLARE bcrypt_hash VARCHAR(100) DEFAULT '$2a$10$qyZA5oVM2fvDilUr9o4asuHgKbbZbt2tKEHnfxzAQ7.eQWIFQjkHu';
    
    -- Admin accounts (5)
    SET i = 1;
    WHILE i <= 5 DO
        INSERT INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID(),
            CONCAT('admin', i, '@hms.com'),
            bcrypt_hash,
            'ADMIN',
            TRUE
        ) ON DUPLICATE KEY UPDATE password = bcrypt_hash;
        SET i = i + 1;
    END WHILE;
    
    -- Doctor accounts (60)
    SET i = 1;
    WHILE i <= 60 DO
        INSERT INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID(),
            CONCAT('doctor', i, '@hms.com'),
            bcrypt_hash,
            'DOCTOR',
            TRUE
        ) ON DUPLICATE KEY UPDATE password = bcrypt_hash;
        SET i = i + 1;
    END WHILE;
    
    -- Nurse accounts (50)
    SET i = 1;
    WHILE i <= 50 DO
        INSERT INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID(),
            CONCAT('nurse', i, '@hms.com'),
            bcrypt_hash,
            'NURSE',
            TRUE
        ) ON DUPLICATE KEY UPDATE password = bcrypt_hash;
        SET i = i + 1;
    END WHILE;
    
    -- Receptionist accounts (40)
    SET i = 1;
    WHILE i <= 40 DO
        INSERT INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID(),
            CONCAT('receptionist', i, '@hms.com'),
            bcrypt_hash,
            'RECEPTIONIST',
            TRUE
        ) ON DUPLICATE KEY UPDATE password = bcrypt_hash;
        SET i = i + 1;
    END WHILE;
    
    -- Patient accounts (1000)
    SET i = 1;
    WHILE i <= 1000 DO
        INSERT INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID(),
            CONCAT('patient', i, '@email.com'),
            bcrypt_hash,
            'PATIENT',
            TRUE
        ) ON DUPLICATE KEY UPDATE password = bcrypt_hash;
        SET i = i + 1;
    END WHILE;
    
    SELECT 'Seeding complete!' AS status;
END//

DELIMITER ;

-- Execute the procedure
CALL seed_accounts();

-- Verify counts
SELECT role, COUNT(*) as count FROM accounts GROUP BY role ORDER BY role;
