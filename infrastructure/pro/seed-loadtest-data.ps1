# ============================================================================
# HMS Backend - Load Test Data Seeding Script (MySQL Version)
# ============================================================================
# Purpose: Execute SQL seed scripts against Docker MySQL containers
# 
# Usage: .\seed-loadtest-data.ps1 [-Reset] [-Verify]
#   -Reset   : Drop and recreate tables before seeding
#   -Verify  : Only run verification queries
#
# Prerequisites:
#   - Docker Desktop running
#   - MySQL containers running for each service
# ============================================================================

param(
    [switch]$Reset,
    [switch]$Verify
)

$ErrorActionPreference = "Stop"

# Configuration
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$SQL_FILE = Join-Path $SCRIPT_DIR "seed-loadtest-data.sql"

# Service Database Configuration
# Map service names to their Docker container names and database names
# Container names and credentials match .env.example configuration
$SERVICES = @{
    "auth-service" = @{
        Container = "mysql-auth-service"
        Database = "auth_db"
        User = "myuser"
        Password = "secret"
        Tables = @("accounts")
    }
    "patient-service" = @{
        Container = "mysql-patient-service"
        Database = "patient_db"
        User = "myuser"
        Password = "secret"
        Tables = @("patient")
    }
    "hr-service" = @{
        Container = "mysql-hr-service"
        Database = "hr_db"
        User = "myuser"
        Password = "secret"
        Tables = @("departments", "employees", "employee_schedules")
    }
    "medicine-service" = @{
        Container = "mysql-medicine-service"
        Database = "medicine_db"
        User = "myuser"
        Password = "secret"
        Tables = @("category", "medicine")
    }
    "medical-exam-service" = @{
        Container = "mysql-medical-exam-service"
        Database = "medical_exam_db"
        User = "myuser"
        Password = "secret"
        Tables = @("lab_tests")
    }
    "appointment-service" = @{
        Container = "mysql-appointment-service"
        Database = "appointment_db"
        User = "myuser"
        Password = "secret"
        Tables = @("appointment")
    }
}

function Write-Header {
    param([string]$Message)
    Write-Host "`n============================================" -ForegroundColor Cyan
    Write-Host $Message -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Test-ContainerRunning {
    param([string]$ContainerName)
    $result = docker ps --filter "name=$ContainerName" --format "{{.Names}}" 2>$null
    return $result -eq $ContainerName
}

function Execute-SQL {
    param(
        [string]$Container,
        [string]$Database,
        [string]$User,
        [string]$SQL
    )
    $ErrorActionPreference = 'SilentlyContinue'
    $result = $SQL | docker exec -i $Container mysql -u $User -psecret $Database 2>&1 | Where-Object { $_ -notmatch '\[Warning\]' }
    $ErrorActionPreference = 'Continue'
    return $result
}

function Get-TableCount {
    param(
        [string]$Container,
        [string]$Database,
        [string]$User,
        [string]$Table
    )
    $sql = "SELECT COUNT(*) FROM $Table;"
    $result = Execute-SQL -Container $Container -Database $Database -User $User -SQL $sql
    if ($result) {
        $resultStr = $result -join "`n"
        if ($resultStr -match '(\d+)' -and $matches) {
            return [int]$matches[1]
        }
    }
    return 0
}

# ============================================================================
# Main Execution
# ============================================================================

Write-Header "HMS Backend - Load Test Data Seeding (MySQL)"

# Check Docker
Write-Info "Checking Docker status..."
try {
    $dockerVersion = docker --version
    Write-Success "Docker is available: $dockerVersion"
} catch {
    Write-Error "Docker is not available. Please start Docker Desktop."
    exit 1
}

# Verify containers are running
Write-Header "Verifying Database Containers"
$allRunning = $true
foreach ($serviceName in $SERVICES.Keys) {
    $config = $SERVICES[$serviceName]
    $isRunning = Test-ContainerRunning -ContainerName $config.Container
    if ($isRunning) {
        Write-Success "$serviceName ($($config.Container)) is running"
    } else {
        Write-Error "$serviceName ($($config.Container)) is NOT running"
        $allRunning = $false
    }
}

if (-not $allRunning) {
    Write-Host "`n" -NoNewline
    Write-Error "Some database containers are not running."
    Write-Info "Please start containers with: docker-compose -f infrastructure/pro/docker-compose.yml up -d"
    exit 1
}

# Verification only mode
if ($Verify) {
    Write-Header "Verifying Existing Data"
    foreach ($serviceName in $SERVICES.Keys) {
        $config = $SERVICES[$serviceName]
        Write-Info "Service: $serviceName"
        foreach ($table in $config.Tables) {
            $count = Get-TableCount -Container $config.Container -Database $config.Database -User $config.User -Table $table
            Write-Host "  - $table`: $count records"
        }
    }
    exit 0
}

# Reset mode - Truncate tables
if ($Reset) {
    Write-Header "Resetting Tables (Truncate)"
    foreach ($serviceName in $SERVICES.Keys) {
        $config = $SERVICES[$serviceName]
        Write-Info "Truncating tables in $serviceName..."
        
        $truncateSQL = "SET FOREIGN_KEY_CHECKS = 0;`n"
        foreach ($table in $config.Tables) {
            $truncateSQL += "TRUNCATE TABLE $table;`n"
        }
        $truncateSQL += "SET FOREIGN_KEY_CHECKS = 1;"
        
        Execute-SQL -Container $config.Container -Database $config.Database -User $config.User -SQL $truncateSQL
        Write-Success "Tables truncated in $serviceName"
    }
}

# ============================================================================
# Execute Seed Scripts per Service
# ============================================================================

Write-Header "Seeding Auth Service"
$authSQL = @"
-- UUID Generation: Using MD5 hash for deterministic, reproducible UUIDs
-- Password: 'Password@123' BCrypt encoded
-- MySQL version using recursive CTE for series generation

-- Create temporary procedure to generate series
DROP PROCEDURE IF EXISTS seed_accounts;
DELIMITER //
CREATE PROCEDURE seed_accounts()
BEGIN
    DECLARE i INT DEFAULT 1;
    
    -- Admins (5)
    WHILE i <= 5 DO
        INSERT IGNORE INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID_TO_BIN(CONCAT(SUBSTR(MD5(CONCAT('admin-', i)), 1, 8), '-', SUBSTR(MD5(CONCAT('admin-', i)), 9, 4), '-', SUBSTR(MD5(CONCAT('admin-', i)), 13, 4), '-', SUBSTR(MD5(CONCAT('admin-', i)), 17, 4), '-', SUBSTR(MD5(CONCAT('admin-', i)), 21, 12))),
            CONCAT('admin', i, '@hms.com'),
            '\$2a\$10\$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
            'ADMIN', true
        );
        SET i = i + 1;
    END WHILE;
    
    -- Doctors (60)
    SET i = 1;
    WHILE i <= 60 DO
        INSERT IGNORE INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID_TO_BIN(CONCAT(SUBSTR(MD5(CONCAT('doctor-', i)), 1, 8), '-', SUBSTR(MD5(CONCAT('doctor-', i)), 9, 4), '-', SUBSTR(MD5(CONCAT('doctor-', i)), 13, 4), '-', SUBSTR(MD5(CONCAT('doctor-', i)), 17, 4), '-', SUBSTR(MD5(CONCAT('doctor-', i)), 21, 12))),
            CONCAT('doctor', i, '@hms.com'),
            '\$2a\$10\$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
            'DOCTOR', true
        );
        SET i = i + 1;
    END WHILE;
    
    -- Nurses (50)
    SET i = 1;
    WHILE i <= 50 DO
        INSERT IGNORE INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID_TO_BIN(CONCAT(SUBSTR(MD5(CONCAT('nurse-', i)), 1, 8), '-', SUBSTR(MD5(CONCAT('nurse-', i)), 9, 4), '-', SUBSTR(MD5(CONCAT('nurse-', i)), 13, 4), '-', SUBSTR(MD5(CONCAT('nurse-', i)), 17, 4), '-', SUBSTR(MD5(CONCAT('nurse-', i)), 21, 12))),
            CONCAT('nurse', i, '@hms.com'),
            '\$2a\$10\$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
            'NURSE', true
        );
        SET i = i + 1;
    END WHILE;
    
    -- Receptionists (40)
    SET i = 1;
    WHILE i <= 40 DO
        INSERT IGNORE INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID_TO_BIN(CONCAT(SUBSTR(MD5(CONCAT('receptionist-', i)), 1, 8), '-', SUBSTR(MD5(CONCAT('receptionist-', i)), 9, 4), '-', SUBSTR(MD5(CONCAT('receptionist-', i)), 13, 4), '-', SUBSTR(MD5(CONCAT('receptionist-', i)), 17, 4), '-', SUBSTR(MD5(CONCAT('receptionist-', i)), 21, 12))),
            CONCAT('receptionist', i, '@hms.com'),
            '\$2a\$10\$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
            'RECEPTIONIST', true
        );
        SET i = i + 1;
    END WHILE;
    
    -- Patients (1000)
    SET i = 1;
    WHILE i <= 1000 DO
        INSERT IGNORE INTO accounts (id, email, password, role, email_verified)
        VALUES (
            UUID_TO_BIN(CONCAT(SUBSTR(MD5(CONCAT('patient-', i)), 1, 8), '-', SUBSTR(MD5(CONCAT('patient-', i)), 9, 4), '-', SUBSTR(MD5(CONCAT('patient-', i)), 13, 4), '-', SUBSTR(MD5(CONCAT('patient-', i)), 17, 4), '-', SUBSTR(MD5(CONCAT('patient-', i)), 21, 12))),
            CONCAT('patient', i, '@email.com'),
            '\$2a\$10\$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
            'PATIENT', true
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;
CALL seed_accounts();
DROP PROCEDURE IF EXISTS seed_accounts;
"@

Execute-SQL -Container $SERVICES["auth-service"].Container -Database $SERVICES["auth-service"].Database -User $SERVICES["auth-service"].User -SQL $authSQL
Write-Success "Auth service seeded"

Write-Header "Seeding HR Service"
$hrSQL = @"
-- Departments (10) - MySQL version
INSERT IGNORE INTO departments (id, name, description, location, phone_extension, status, created_at, updated_at, created_by, updated_by)
VALUES
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-1'), 1, 8), '-', SUBSTR(MD5('dept-1'), 9, 4), '-', SUBSTR(MD5('dept-1'), 13, 4), '-', SUBSTR(MD5('dept-1'), 17, 4), '-', SUBSTR(MD5('dept-1'), 21, 12)), '-', '')), 'Internal Medicine', 'General internal medicine department', 'Building A, Floor 1', '1001', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-2'), 1, 8), '-', SUBSTR(MD5('dept-2'), 9, 4), '-', SUBSTR(MD5('dept-2'), 13, 4), '-', SUBSTR(MD5('dept-2'), 17, 4), '-', SUBSTR(MD5('dept-2'), 21, 12)), '-', '')), 'Cardiology', 'Heart and cardiovascular diseases', 'Building A, Floor 2', '1002', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-3'), 1, 8), '-', SUBSTR(MD5('dept-3'), 9, 4), '-', SUBSTR(MD5('dept-3'), 13, 4), '-', SUBSTR(MD5('dept-3'), 17, 4), '-', SUBSTR(MD5('dept-3'), 21, 12)), '-', '')), 'Orthopedics', 'Bone and joint treatment', 'Building B, Floor 1', '1003', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-4'), 1, 8), '-', SUBSTR(MD5('dept-4'), 9, 4), '-', SUBSTR(MD5('dept-4'), 13, 4), '-', SUBSTR(MD5('dept-4'), 17, 4), '-', SUBSTR(MD5('dept-4'), 21, 12)), '-', '')), 'Pediatrics', 'Child healthcare', 'Building B, Floor 2', '1004', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-5'), 1, 8), '-', SUBSTR(MD5('dept-5'), 9, 4), '-', SUBSTR(MD5('dept-5'), 13, 4), '-', SUBSTR(MD5('dept-5'), 17, 4), '-', SUBSTR(MD5('dept-5'), 21, 12)), '-', '')), 'Dermatology', 'Skin conditions treatment', 'Building C, Floor 1', '1005', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-6'), 1, 8), '-', SUBSTR(MD5('dept-6'), 9, 4), '-', SUBSTR(MD5('dept-6'), 13, 4), '-', SUBSTR(MD5('dept-6'), 17, 4), '-', SUBSTR(MD5('dept-6'), 21, 12)), '-', '')), 'Neurology', 'Brain and nervous system', 'Building C, Floor 2', '1006', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-7'), 1, 8), '-', SUBSTR(MD5('dept-7'), 9, 4), '-', SUBSTR(MD5('dept-7'), 13, 4), '-', SUBSTR(MD5('dept-7'), 17, 4), '-', SUBSTR(MD5('dept-7'), 21, 12)), '-', '')), 'Ophthalmology', 'Eye care and treatment', 'Building D, Floor 1', '1007', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-8'), 1, 8), '-', SUBSTR(MD5('dept-8'), 9, 4), '-', SUBSTR(MD5('dept-8'), 13, 4), '-', SUBSTR(MD5('dept-8'), 17, 4), '-', SUBSTR(MD5('dept-8'), 21, 12)), '-', '')), 'ENT', 'Ear, Nose and Throat', 'Building D, Floor 2', '1008', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-9'), 1, 8), '-', SUBSTR(MD5('dept-9'), 9, 4), '-', SUBSTR(MD5('dept-9'), 13, 4), '-', SUBSTR(MD5('dept-9'), 17, 4), '-', SUBSTR(MD5('dept-9'), 21, 12)), '-', '')), 'Gynecology', 'Women health', 'Building E, Floor 1', '1009', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-10'), 1, 8), '-', SUBSTR(MD5('dept-10'), 9, 4), '-', SUBSTR(MD5('dept-10'), 13, 4), '-', SUBSTR(MD5('dept-10'), 17, 4), '-', SUBSTR(MD5('dept-10'), 21, 12)), '-', '')), 'General Surgery', 'Surgical procedures', 'Building E, Floor 2', '1010', 'ACTIVE', NOW(), NOW(), 'system', 'system');
"@

Execute-SQL -Container $SERVICES["hr-service"].Container -Database $SERVICES["hr-service"].Database -User $SERVICES["hr-service"].User -SQL $hrSQL

# HR Employees via PowerShell loops to avoid MySQL procedure issues
$specializations = @('Cardiology', 'Neurology', 'Orthopedics', 'Pediatrics', 'Dermatology', 'Internal Medicine', 'General Surgery', 'Ophthalmology', 'ENT', 'Gynecology')

# Doctors (60)
$doctorSQL = ""
for ($i = 1; $i -le 60; $i++) {
    $deptNum = 1 + (($i - 1) % 10)
    $spec = $specializations[($i - 1) % 10]
    $lic = "LIC" + $i.ToString().PadLeft(6, '0')
    $phone = "090" + (1000000 + $i).ToString().PadLeft(7, '0')
    $doctorSQL += "INSERT IGNORE INTO employees (id, account_id, full_name, role, department_id, specialization, license_number, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('emp-doctor-$i'), 1, 8), '-', SUBSTR(MD5('emp-doctor-$i'), 9, 4), '-', SUBSTR(MD5('emp-doctor-$i'), 13, 4), '-', SUBSTR(MD5('emp-doctor-$i'), 17, 4), '-', SUBSTR(MD5('emp-doctor-$i'), 21, 12)), '-', '')), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('doctor-$i'), 1, 8), '-', SUBSTR(MD5('doctor-$i'), 9, 4), '-', SUBSTR(MD5('doctor-$i'), 13, 4), '-', SUBSTR(MD5('doctor-$i'), 17, 4), '-', SUBSTR(MD5('doctor-$i'), 21, 12)), '-', '')), 'Dr. Nguyen Van $i', 'DOCTOR', UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-$deptNum'), 1, 8), '-', SUBSTR(MD5('dept-$deptNum'), 9, 4), '-', SUBSTR(MD5('dept-$deptNum'), 13, 4), '-', SUBSTR(MD5('dept-$deptNum'), 17, 4), '-', SUBSTR(MD5('dept-$deptNum'), 21, 12)), '-', '')), '$spec', '$lic', '$phone', '$i Le Loi Street, HCMC', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 YEAR), NOW(), NOW(), 'system', 'system');`n"
}
Execute-SQL -Container $SERVICES["hr-service"].Container -Database $SERVICES["hr-service"].Database -User $SERVICES["hr-service"].User -SQL $doctorSQL

# Nurses (50)
$nurseSQL = ""
for ($i = 1; $i -le 50; $i++) {
    $deptNum = 1 + (($i - 1) % 10)
    $phone = "091" + (2000000 + $i).ToString().PadLeft(7, '0')
    $nurseSQL += "INSERT IGNORE INTO employees (id, account_id, full_name, role, department_id, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('emp-nurse-$i'), 1, 8), '-', SUBSTR(MD5('emp-nurse-$i'), 9, 4), '-', SUBSTR(MD5('emp-nurse-$i'), 13, 4), '-', SUBSTR(MD5('emp-nurse-$i'), 17, 4), '-', SUBSTR(MD5('emp-nurse-$i'), 21, 12)), '-', '')), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('nurse-$i'), 1, 8), '-', SUBSTR(MD5('nurse-$i'), 9, 4), '-', SUBSTR(MD5('nurse-$i'), 13, 4), '-', SUBSTR(MD5('nurse-$i'), 17, 4), '-', SUBSTR(MD5('nurse-$i'), 21, 12)), '-', '')), 'Nurse Tran Thi $i', 'NURSE', UNHEX(REPLACE(CONCAT(SUBSTR(MD5('dept-$deptNum'), 1, 8), '-', SUBSTR(MD5('dept-$deptNum'), 9, 4), '-', SUBSTR(MD5('dept-$deptNum'), 13, 4), '-', SUBSTR(MD5('dept-$deptNum'), 17, 4), '-', SUBSTR(MD5('dept-$deptNum'), 21, 12)), '-', '')), '$phone', '$i Nguyen Hue Street, HCMC', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 3 YEAR), NOW(), NOW(), 'system', 'system');`n"
}
Execute-SQL -Container $SERVICES["hr-service"].Container -Database $SERVICES["hr-service"].Database -User $SERVICES["hr-service"].User -SQL $nurseSQL

# Receptionists (40)
$receptionistSQL = ""
for ($i = 1; $i -le 40; $i++) {
    $phone = "092" + (3000000 + $i).ToString().PadLeft(7, '0')
    $receptionistSQL += "INSERT IGNORE INTO employees (id, account_id, full_name, role, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('emp-receptionist-$i'), 1, 8), '-', SUBSTR(MD5('emp-receptionist-$i'), 9, 4), '-', SUBSTR(MD5('emp-receptionist-$i'), 13, 4), '-', SUBSTR(MD5('emp-receptionist-$i'), 17, 4), '-', SUBSTR(MD5('emp-receptionist-$i'), 21, 12)), '-', '')), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('receptionist-$i'), 1, 8), '-', SUBSTR(MD5('receptionist-$i'), 9, 4), '-', SUBSTR(MD5('receptionist-$i'), 13, 4), '-', SUBSTR(MD5('receptionist-$i'), 17, 4), '-', SUBSTR(MD5('receptionist-$i'), 21, 12)), '-', '')), 'Receptionist Le Kim $i', 'RECEPTIONIST', '$phone', '$i Tran Hung Dao Street, HCMC', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 YEAR), NOW(), NOW(), 'system', 'system');`n"
}
Execute-SQL -Container $SERVICES["hr-service"].Container -Database $SERVICES["hr-service"].Database -User $SERVICES["hr-service"].User -SQL $receptionistSQL

# Admins (5)
$adminSQL = ""
for ($i = 1; $i -le 5; $i++) {
    $phone = "093" + (4000000 + $i).ToString().PadLeft(7, '0')
    $adminSQL += "INSERT IGNORE INTO employees (id, account_id, full_name, role, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('emp-admin-$i'), 1, 8), '-', SUBSTR(MD5('emp-admin-$i'), 9, 4), '-', SUBSTR(MD5('emp-admin-$i'), 13, 4), '-', SUBSTR(MD5('emp-admin-$i'), 17, 4), '-', SUBSTR(MD5('emp-admin-$i'), 21, 12)), '-', '')), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('admin-$i'), 1, 8), '-', SUBSTR(MD5('admin-$i'), 9, 4), '-', SUBSTR(MD5('admin-$i'), 13, 4), '-', SUBSTR(MD5('admin-$i'), 17, 4), '-', SUBSTR(MD5('admin-$i'), 21, 12)), '-', '')), 'Admin Pham Van $i', 'ADMIN', '$phone', '$i Pasteur Street, HCMC', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 5 YEAR), NOW(), NOW(), 'system', 'system');`n"
}
Execute-SQL -Container $SERVICES["hr-service"].Container -Database $SERVICES["hr-service"].Database -User $SERVICES["hr-service"].User -SQL $adminSQL

# Employee Schedules (60 doctors x 7 days = 420)
$scheduleSQL = ""
for ($doc = 1; $doc -le 60; $doc++) {
    for ($day = 1; $day -le 7; $day++) {
        $scheduleSQL += "INSERT IGNORE INTO employee_schedules (id, employee_id, work_date, start_time, end_time, status, notes, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('schedule-$doc-$day'), 1, 8), '-', SUBSTR(MD5('schedule-$doc-$day'), 9, 4), '-', SUBSTR(MD5('schedule-$doc-$day'), 13, 4), '-', SUBSTR(MD5('schedule-$doc-$day'), 17, 4), '-', SUBSTR(MD5('schedule-$doc-$day'), 21, 12)), '-', '')), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('emp-doctor-$doc'), 1, 8), '-', SUBSTR(MD5('emp-doctor-$doc'), 9, 4), '-', SUBSTR(MD5('emp-doctor-$doc'), 13, 4), '-', SUBSTR(MD5('emp-doctor-$doc'), 17, 4), '-', SUBSTR(MD5('emp-doctor-$doc'), 21, 12)), '-', '')), DATE_ADD(CURDATE(), INTERVAL $day DAY), '08:00:00', '17:00:00', 'AVAILABLE', 'Regular work day', NOW(), NOW(), 'system', 'system');`n"
    }
}
Execute-SQL -Container $SERVICES["hr-service"].Container -Database $SERVICES["hr-service"].Database -User $SERVICES["hr-service"].User -SQL $scheduleSQL
Write-Success "HR service seeded"

Write-Header "Seeding Patient Service"
# Patients (1000) via PowerShell loop for MySQL
$genders = @('MALE', 'FEMALE', 'OTHER')
$bloodTypes = @('A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-')
$relationships = @('Spouse', 'Parent', 'Child', 'Sibling', 'Friend')

$patientSQL = ""
for ($i = 1; $i -le 1000; $i++) {
    $gender = $genders[($i - 1) % 3]
    $blood = $bloodTypes[($i - 1) % 8]
    $rel = $relationships[($i - 1) % 5]
    $phone = "098" + (5000000 + $i).ToString().PadLeft(7, '0')
    $relPhone = "097" + (6000000 + $i).ToString().PadLeft(7, '0')
    $idNum = "0" + (79000000000 + $i).ToString().PadLeft(11, '0')
    $insNum = "INS" + $i.ToString().PadLeft(8, '0')
    $district = 1 + (($i - 1) % 12)
    $patientSQL += "INSERT IGNORE INTO patient (id, account_id, full_name, email, date_of_birth, gender, phone_number, address, identification_number, health_insurance_number, relative_full_name, relative_phone_number, relative_relationship, blood_type, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('patient-$i'), 1, 8), '-', SUBSTR(MD5('patient-$i'), 9, 4), '-', SUBSTR(MD5('patient-$i'), 13, 4), '-', SUBSTR(MD5('patient-$i'), 17, 4), '-', SUBSTR(MD5('patient-$i'), 21, 12)), '-', '')), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('patient-$i'), 1, 8), '-', SUBSTR(MD5('patient-$i'), 9, 4), '-', SUBSTR(MD5('patient-$i'), 13, 4), '-', SUBSTR(MD5('patient-$i'), 17, 4), '-', SUBSTR(MD5('patient-$i'), 21, 12)), '-', '')), 'Patient Nguyen Van $i', 'patient$i@email.com', DATE_SUB(CURDATE(), INTERVAL FLOOR(20 + RAND() * 50) YEAR), '$gender', '$phone', '$i Street Name, District $district, HCMC', '$idNum', '$insNum', 'Emergency Contact $i', '$relPhone', '$rel', '$blood', NOW(), NOW(), 'system', 'system');`n"
}
Execute-SQL -Container $SERVICES["patient-service"].Container -Database $SERVICES["patient-service"].Database -User $SERVICES["patient-service"].User -SQL $patientSQL
Write-Success "Patient service seeded"

Write-Header "Seeding Medicine Service"
$medicineSQL = @"
-- Categories (10) - MySQL version
INSERT IGNORE INTO category (id, name, description, created_at, updated_at, created_by, updated_by)
VALUES
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-1'), 1, 8), '-', SUBSTR(MD5('cat-1'), 9, 4), '-', SUBSTR(MD5('cat-1'), 13, 4), '-', SUBSTR(MD5('cat-1'), 17, 4), '-', SUBSTR(MD5('cat-1'), 21, 12)), '-', '')), 'Antibiotics', 'Medicines that fight bacterial infections', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-2'), 1, 8), '-', SUBSTR(MD5('cat-2'), 9, 4), '-', SUBSTR(MD5('cat-2'), 13, 4), '-', SUBSTR(MD5('cat-2'), 17, 4), '-', SUBSTR(MD5('cat-2'), 21, 12)), '-', '')), 'Analgesics', 'Pain relievers and fever reducers', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-3'), 1, 8), '-', SUBSTR(MD5('cat-3'), 9, 4), '-', SUBSTR(MD5('cat-3'), 13, 4), '-', SUBSTR(MD5('cat-3'), 17, 4), '-', SUBSTR(MD5('cat-3'), 21, 12)), '-', '')), 'Antihypertensives', 'Blood pressure medications', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-4'), 1, 8), '-', SUBSTR(MD5('cat-4'), 9, 4), '-', SUBSTR(MD5('cat-4'), 13, 4), '-', SUBSTR(MD5('cat-4'), 17, 4), '-', SUBSTR(MD5('cat-4'), 21, 12)), '-', '')), 'Antidiabetics', 'Diabetes management medications', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-5'), 1, 8), '-', SUBSTR(MD5('cat-5'), 9, 4), '-', SUBSTR(MD5('cat-5'), 13, 4), '-', SUBSTR(MD5('cat-5'), 17, 4), '-', SUBSTR(MD5('cat-5'), 21, 12)), '-', '')), 'Antihistamines', 'Allergy medications', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-6'), 1, 8), '-', SUBSTR(MD5('cat-6'), 9, 4), '-', SUBSTR(MD5('cat-6'), 13, 4), '-', SUBSTR(MD5('cat-6'), 17, 4), '-', SUBSTR(MD5('cat-6'), 21, 12)), '-', '')), 'Cardiovascular', 'Heart and blood vessel medications', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-7'), 1, 8), '-', SUBSTR(MD5('cat-7'), 9, 4), '-', SUBSTR(MD5('cat-7'), 13, 4), '-', SUBSTR(MD5('cat-7'), 17, 4), '-', SUBSTR(MD5('cat-7'), 21, 12)), '-', '')), 'Gastrointestinal', 'Digestive system medications', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-8'), 1, 8), '-', SUBSTR(MD5('cat-8'), 9, 4), '-', SUBSTR(MD5('cat-8'), 13, 4), '-', SUBSTR(MD5('cat-8'), 17, 4), '-', SUBSTR(MD5('cat-8'), 21, 12)), '-', '')), 'Vitamins', 'Vitamin and mineral supplements', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-9'), 1, 8), '-', SUBSTR(MD5('cat-9'), 9, 4), '-', SUBSTR(MD5('cat-9'), 13, 4), '-', SUBSTR(MD5('cat-9'), 17, 4), '-', SUBSTR(MD5('cat-9'), 21, 12)), '-', '')), 'Dermatological', 'Skin treatment medications', NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-10'), 1, 8), '-', SUBSTR(MD5('cat-10'), 9, 4), '-', SUBSTR(MD5('cat-10'), 13, 4), '-', SUBSTR(MD5('cat-10'), 17, 4), '-', SUBSTR(MD5('cat-10'), 21, 12)), '-', '')), 'Respiratory', 'Respiratory system medications', NOW(), NOW(), 'system', 'system');
"@

Execute-SQL -Container $SERVICES["medicine-service"].Container -Database $SERVICES["medicine-service"].Database -User $SERVICES["medicine-service"].User -SQL $medicineSQL

# Medicines (200) via PowerShell loop
$medNames = @('Amoxicillin', 'Paracetamol', 'Ibuprofen', 'Omeprazole', 'Metformin', 'Lisinopril', 'Amlodipine', 'Losartan', 'Atorvastatin', 'Cetirizine')
$manufacturers = @('Sanofi', 'Pfizer', 'Novartis', 'Roche', 'GSK', 'AstraZeneca', 'JnJ', 'Merck', 'Abbott', 'Bayer')
$units = @('tablet', 'capsule', 'ml', 'mg', 'sachet')

$medSQL = ""
for ($i = 1; $i -le 200; $i++) {
    $medName = $medNames[($i - 1) % 10]
    $dosage = ([math]::Floor($i / 10) + 1) * 100
    $mfr = $manufacturers[($i - 1) % 10]
    $unit = $units[($i - 1) % 5]
    $catNum = 1 + (($i - 1) % 10)
    $qty = 100 + ($i % 900)
    $purchase = 5000 + ($i * 100)
    $selling = 10000 + ($i * 200)
    $tabCount = 10 + (($i % 5) * 10)
    $medSQL += "INSERT IGNORE INTO medicine (id, name, active_ingredient, unit, description, quantity, concentration, packaging, purchase_price, selling_price, manufacturer, side_effects, storage_conditions, expires_at, category_id, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('med-$i'), 1, 8), '-', SUBSTR(MD5('med-$i'), 9, 4), '-', SUBSTR(MD5('med-$i'), 13, 4), '-', SUBSTR(MD5('med-$i'), 17, 4), '-', SUBSTR(MD5('med-$i'), 21, 12)), '-', '')), '$medName ${dosage}mg', '$medName', '$unit', 'Description for medicine $i', $qty, '${dosage}mg', 'Box of $tabCount tablets', $purchase, $selling, '$mfr', 'May cause drowsiness', 'Store in cool, dry place', DATE_ADD(NOW(), INTERVAL 2 YEAR), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('cat-$catNum'), 1, 8), '-', SUBSTR(MD5('cat-$catNum'), 9, 4), '-', SUBSTR(MD5('cat-$catNum'), 13, 4), '-', SUBSTR(MD5('cat-$catNum'), 17, 4), '-', SUBSTR(MD5('cat-$catNum'), 21, 12)), '-', '')), NOW(), NOW(), 'system', 'system');`n"
}
Execute-SQL -Container $SERVICES["medicine-service"].Container -Database $SERVICES["medicine-service"].Database -User $SERVICES["medicine-service"].User -SQL $medSQL
Write-Success "Medicine service seeded"

Write-Header "Seeding Medical Exam Service"
$labTestSQL = @"
-- Lab Tests (50) - MySQL version
INSERT IGNORE INTO lab_tests (id, code, name, category, description, price, unit, normal_range, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-1'), 1, 8), '-', SUBSTR(MD5('labtest-1'), 9, 4), '-', SUBSTR(MD5('labtest-1'), 13, 4), '-', SUBSTR(MD5('labtest-1'), 17, 4), '-', SUBSTR(MD5('labtest-1'), 21, 12)), '-', '')), 'CBC', 'Complete Blood Count', 'LAB', 'Full blood cell analysis', 150000, 'cells/uL', '4.5-11.0', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-2'), 1, 8), '-', SUBSTR(MD5('labtest-2'), 9, 4), '-', SUBSTR(MD5('labtest-2'), 13, 4), '-', SUBSTR(MD5('labtest-2'), 17, 4), '-', SUBSTR(MD5('labtest-2'), 21, 12)), '-', '')), 'BMP', 'Basic Metabolic Panel', 'LAB', 'Kidney function and electrolytes', 200000, 'mmol/L', 'varies', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-3'), 1, 8), '-', SUBSTR(MD5('labtest-3'), 9, 4), '-', SUBSTR(MD5('labtest-3'), 13, 4), '-', SUBSTR(MD5('labtest-3'), 17, 4), '-', SUBSTR(MD5('labtest-3'), 21, 12)), '-', '')), 'CMP', 'Comprehensive Metabolic Panel', 'LAB', 'Extended metabolic test', 350000, 'mmol/L', 'varies', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-4'), 1, 8), '-', SUBSTR(MD5('labtest-4'), 9, 4), '-', SUBSTR(MD5('labtest-4'), 13, 4), '-', SUBSTR(MD5('labtest-4'), 17, 4), '-', SUBSTR(MD5('labtest-4'), 21, 12)), '-', '')), 'LIPID', 'Lipid Panel', 'LAB', 'Cholesterol and triglycerides', 180000, 'mg/dL', '<200', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-5'), 1, 8), '-', SUBSTR(MD5('labtest-5'), 9, 4), '-', SUBSTR(MD5('labtest-5'), 13, 4), '-', SUBSTR(MD5('labtest-5'), 17, 4), '-', SUBSTR(MD5('labtest-5'), 21, 12)), '-', '')), 'LFT', 'Liver Function Tests', 'LAB', 'Liver enzyme analysis', 220000, 'U/L', 'varies', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-6'), 1, 8), '-', SUBSTR(MD5('labtest-6'), 9, 4), '-', SUBSTR(MD5('labtest-6'), 13, 4), '-', SUBSTR(MD5('labtest-6'), 17, 4), '-', SUBSTR(MD5('labtest-6'), 21, 12)), '-', '')), 'TSH', 'Thyroid Stimulating Hormone', 'LAB', 'Thyroid function', 250000, 'mIU/L', '0.4-4.0', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-7'), 1, 8), '-', SUBSTR(MD5('labtest-7'), 9, 4), '-', SUBSTR(MD5('labtest-7'), 13, 4), '-', SUBSTR(MD5('labtest-7'), 17, 4), '-', SUBSTR(MD5('labtest-7'), 21, 12)), '-', '')), 'HBA1C', 'Hemoglobin A1c', 'LAB', 'Diabetes control marker', 180000, '%', '<5.7', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-8'), 1, 8), '-', SUBSTR(MD5('labtest-8'), 9, 4), '-', SUBSTR(MD5('labtest-8'), 13, 4), '-', SUBSTR(MD5('labtest-8'), 17, 4), '-', SUBSTR(MD5('labtest-8'), 21, 12)), '-', '')), 'FBS', 'Fasting Blood Sugar', 'LAB', 'Glucose level', 80000, 'mg/dL', '70-100', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-9'), 1, 8), '-', SUBSTR(MD5('labtest-9'), 9, 4), '-', SUBSTR(MD5('labtest-9'), 13, 4), '-', SUBSTR(MD5('labtest-9'), 17, 4), '-', SUBSTR(MD5('labtest-9'), 21, 12)), '-', '')), 'UA', 'Urinalysis', 'LAB', 'Urine analysis', 100000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-10'), 1, 8), '-', SUBSTR(MD5('labtest-10'), 9, 4), '-', SUBSTR(MD5('labtest-10'), 13, 4), '-', SUBSTR(MD5('labtest-10'), 17, 4), '-', SUBSTR(MD5('labtest-10'), 21, 12)), '-', '')), 'PT', 'Prothrombin Time', 'LAB', 'Blood clotting test', 150000, 'seconds', '11-13.5', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-11'), 1, 8), '-', SUBSTR(MD5('labtest-11'), 9, 4), '-', SUBSTR(MD5('labtest-11'), 13, 4), '-', SUBSTR(MD5('labtest-11'), 17, 4), '-', SUBSTR(MD5('labtest-11'), 21, 12)), '-', '')), 'BUN', 'Blood Urea Nitrogen', 'LAB', 'Kidney function marker', 120000, 'mg/dL', '7-20', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-12'), 1, 8), '-', SUBSTR(MD5('labtest-12'), 9, 4), '-', SUBSTR(MD5('labtest-12'), 13, 4), '-', SUBSTR(MD5('labtest-12'), 17, 4), '-', SUBSTR(MD5('labtest-12'), 21, 12)), '-', '')), 'CREAT', 'Creatinine', 'LAB', 'Kidney function marker', 120000, 'mg/dL', '0.7-1.3', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-13'), 1, 8), '-', SUBSTR(MD5('labtest-13'), 9, 4), '-', SUBSTR(MD5('labtest-13'), 13, 4), '-', SUBSTR(MD5('labtest-13'), 17, 4), '-', SUBSTR(MD5('labtest-13'), 21, 12)), '-', '')), 'ESR', 'Erythrocyte Sedimentation Rate', 'LAB', 'Inflammation marker', 100000, 'mm/hr', '<20', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-14'), 1, 8), '-', SUBSTR(MD5('labtest-14'), 9, 4), '-', SUBSTR(MD5('labtest-14'), 13, 4), '-', SUBSTR(MD5('labtest-14'), 17, 4), '-', SUBSTR(MD5('labtest-14'), 21, 12)), '-', '')), 'CRP', 'C-Reactive Protein', 'LAB', 'Inflammation marker', 150000, 'mg/L', '<3', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-15'), 1, 8), '-', SUBSTR(MD5('labtest-15'), 9, 4), '-', SUBSTR(MD5('labtest-15'), 13, 4), '-', SUBSTR(MD5('labtest-15'), 17, 4), '-', SUBSTR(MD5('labtest-15'), 21, 12)), '-', '')), 'FERR', 'Ferritin', 'LAB', 'Iron storage', 180000, 'ng/mL', '12-150', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-16'), 1, 8), '-', SUBSTR(MD5('labtest-16'), 9, 4), '-', SUBSTR(MD5('labtest-16'), 13, 4), '-', SUBSTR(MD5('labtest-16'), 17, 4), '-', SUBSTR(MD5('labtest-16'), 21, 12)), '-', '')), 'VITD', 'Vitamin D', 'LAB', '25-hydroxy vitamin D', 300000, 'ng/mL', '30-100', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-17'), 1, 8), '-', SUBSTR(MD5('labtest-17'), 9, 4), '-', SUBSTR(MD5('labtest-17'), 13, 4), '-', SUBSTR(MD5('labtest-17'), 17, 4), '-', SUBSTR(MD5('labtest-17'), 21, 12)), '-', '')), 'VITB12', 'Vitamin B12', 'LAB', 'Cobalamin level', 250000, 'pg/mL', '200-900', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-18'), 1, 8), '-', SUBSTR(MD5('labtest-18'), 9, 4), '-', SUBSTR(MD5('labtest-18'), 13, 4), '-', SUBSTR(MD5('labtest-18'), 17, 4), '-', SUBSTR(MD5('labtest-18'), 21, 12)), '-', '')), 'URIC', 'Uric Acid', 'LAB', 'Gout marker', 120000, 'mg/dL', '3.5-7.2', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-19'), 1, 8), '-', SUBSTR(MD5('labtest-19'), 9, 4), '-', SUBSTR(MD5('labtest-19'), 13, 4), '-', SUBSTR(MD5('labtest-19'), 17, 4), '-', SUBSTR(MD5('labtest-19'), 21, 12)), '-', '')), 'HIV', 'HIV Antibody Test', 'LAB', 'HIV screening', 200000, NULL, 'Negative', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-20'), 1, 8), '-', SUBSTR(MD5('labtest-20'), 9, 4), '-', SUBSTR(MD5('labtest-20'), 13, 4), '-', SUBSTR(MD5('labtest-20'), 17, 4), '-', SUBSTR(MD5('labtest-20'), 21, 12)), '-', '')), 'HBSAG', 'Hepatitis B Surface Antigen', 'LAB', 'Hepatitis B screening', 180000, NULL, 'Negative', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-21'), 1, 8), '-', SUBSTR(MD5('labtest-21'), 9, 4), '-', SUBSTR(MD5('labtest-21'), 13, 4), '-', SUBSTR(MD5('labtest-21'), 17, 4), '-', SUBSTR(MD5('labtest-21'), 21, 12)), '-', '')), 'XRAY_CHEST', 'Chest X-Ray', 'IMAGING', 'Chest radiograph', 250000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-22'), 1, 8), '-', SUBSTR(MD5('labtest-22'), 9, 4), '-', SUBSTR(MD5('labtest-22'), 13, 4), '-', SUBSTR(MD5('labtest-22'), 17, 4), '-', SUBSTR(MD5('labtest-22'), 21, 12)), '-', '')), 'XRAY_SPINE', 'Spine X-Ray', 'IMAGING', 'Spinal radiograph', 300000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-23'), 1, 8), '-', SUBSTR(MD5('labtest-23'), 9, 4), '-', SUBSTR(MD5('labtest-23'), 13, 4), '-', SUBSTR(MD5('labtest-23'), 17, 4), '-', SUBSTR(MD5('labtest-23'), 21, 12)), '-', '')), 'XRAY_KNEE', 'Knee X-Ray', 'IMAGING', 'Knee joint radiograph', 200000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-24'), 1, 8), '-', SUBSTR(MD5('labtest-24'), 9, 4), '-', SUBSTR(MD5('labtest-24'), 13, 4), '-', SUBSTR(MD5('labtest-24'), 17, 4), '-', SUBSTR(MD5('labtest-24'), 21, 12)), '-', '')), 'USG_ABDOMEN', 'Abdominal Ultrasound', 'IMAGING', 'Abdominal scan', 400000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-25'), 1, 8), '-', SUBSTR(MD5('labtest-25'), 9, 4), '-', SUBSTR(MD5('labtest-25'), 13, 4), '-', SUBSTR(MD5('labtest-25'), 17, 4), '-', SUBSTR(MD5('labtest-25'), 21, 12)), '-', '')), 'USG_PELVIS', 'Pelvic Ultrasound', 'IMAGING', 'Pelvic scan', 350000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-26'), 1, 8), '-', SUBSTR(MD5('labtest-26'), 9, 4), '-', SUBSTR(MD5('labtest-26'), 13, 4), '-', SUBSTR(MD5('labtest-26'), 17, 4), '-', SUBSTR(MD5('labtest-26'), 21, 12)), '-', '')), 'USG_THYROID', 'Thyroid Ultrasound', 'IMAGING', 'Thyroid scan', 300000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-27'), 1, 8), '-', SUBSTR(MD5('labtest-27'), 9, 4), '-', SUBSTR(MD5('labtest-27'), 13, 4), '-', SUBSTR(MD5('labtest-27'), 17, 4), '-', SUBSTR(MD5('labtest-27'), 21, 12)), '-', '')), 'ECG', 'Electrocardiogram', 'IMAGING', '12-lead ECG', 200000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-28'), 1, 8), '-', SUBSTR(MD5('labtest-28'), 9, 4), '-', SUBSTR(MD5('labtest-28'), 13, 4), '-', SUBSTR(MD5('labtest-28'), 17, 4), '-', SUBSTR(MD5('labtest-28'), 21, 12)), '-', '')), 'ECHO', 'Echocardiogram', 'IMAGING', 'Heart ultrasound', 800000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-29'), 1, 8), '-', SUBSTR(MD5('labtest-29'), 9, 4), '-', SUBSTR(MD5('labtest-29'), 13, 4), '-', SUBSTR(MD5('labtest-29'), 17, 4), '-', SUBSTR(MD5('labtest-29'), 21, 12)), '-', '')), 'CT_HEAD', 'CT Scan Head', 'IMAGING', 'Brain CT', 1500000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-30'), 1, 8), '-', SUBSTR(MD5('labtest-30'), 9, 4), '-', SUBSTR(MD5('labtest-30'), 13, 4), '-', SUBSTR(MD5('labtest-30'), 17, 4), '-', SUBSTR(MD5('labtest-30'), 21, 12)), '-', '')), 'CT_CHEST', 'CT Scan Chest', 'IMAGING', 'Chest CT', 2000000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-31'), 1, 8), '-', SUBSTR(MD5('labtest-31'), 9, 4), '-', SUBSTR(MD5('labtest-31'), 13, 4), '-', SUBSTR(MD5('labtest-31'), 17, 4), '-', SUBSTR(MD5('labtest-31'), 21, 12)), '-', '')), 'CT_ABDOMEN', 'CT Scan Abdomen', 'IMAGING', 'Abdominal CT', 2500000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-32'), 1, 8), '-', SUBSTR(MD5('labtest-32'), 9, 4), '-', SUBSTR(MD5('labtest-32'), 13, 4), '-', SUBSTR(MD5('labtest-32'), 17, 4), '-', SUBSTR(MD5('labtest-32'), 21, 12)), '-', '')), 'MRI_BRAIN', 'MRI Brain', 'IMAGING', 'Brain MRI', 3500000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-33'), 1, 8), '-', SUBSTR(MD5('labtest-33'), 9, 4), '-', SUBSTR(MD5('labtest-33'), 13, 4), '-', SUBSTR(MD5('labtest-33'), 17, 4), '-', SUBSTR(MD5('labtest-33'), 21, 12)), '-', '')), 'MRI_SPINE', 'MRI Spine', 'IMAGING', 'Spinal MRI', 3000000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-34'), 1, 8), '-', SUBSTR(MD5('labtest-34'), 9, 4), '-', SUBSTR(MD5('labtest-34'), 13, 4), '-', SUBSTR(MD5('labtest-34'), 17, 4), '-', SUBSTR(MD5('labtest-34'), 21, 12)), '-', '')), 'MRI_KNEE', 'MRI Knee', 'IMAGING', 'Knee MRI', 2800000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-35'), 1, 8), '-', SUBSTR(MD5('labtest-35'), 9, 4), '-', SUBSTR(MD5('labtest-35'), 13, 4), '-', SUBSTR(MD5('labtest-35'), 17, 4), '-', SUBSTR(MD5('labtest-35'), 21, 12)), '-', '')), 'MAMMO', 'Mammography', 'IMAGING', 'Breast screening', 500000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-36'), 1, 8), '-', SUBSTR(MD5('labtest-36'), 9, 4), '-', SUBSTR(MD5('labtest-36'), 13, 4), '-', SUBSTR(MD5('labtest-36'), 17, 4), '-', SUBSTR(MD5('labtest-36'), 21, 12)), '-', '')), 'DEXA', 'Bone Density Scan', 'IMAGING', 'DEXA scan', 600000, 'T-score', '>-1.0', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-37'), 1, 8), '-', SUBSTR(MD5('labtest-37'), 9, 4), '-', SUBSTR(MD5('labtest-37'), 13, 4), '-', SUBSTR(MD5('labtest-37'), 17, 4), '-', SUBSTR(MD5('labtest-37'), 21, 12)), '-', '')), 'EEG', 'Electroencephalogram', 'IMAGING', 'Brain wave', 700000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-38'), 1, 8), '-', SUBSTR(MD5('labtest-38'), 9, 4), '-', SUBSTR(MD5('labtest-38'), 13, 4), '-', SUBSTR(MD5('labtest-38'), 17, 4), '-', SUBSTR(MD5('labtest-38'), 21, 12)), '-', '')), 'BIOPSY_SKIN', 'Skin Biopsy', 'PATHOLOGY', 'Skin tissue', 500000, NULL, 'Benign', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-39'), 1, 8), '-', SUBSTR(MD5('labtest-39'), 9, 4), '-', SUBSTR(MD5('labtest-39'), 13, 4), '-', SUBSTR(MD5('labtest-39'), 17, 4), '-', SUBSTR(MD5('labtest-39'), 21, 12)), '-', '')), 'BIOPSY_LIVER', 'Liver Biopsy', 'PATHOLOGY', 'Liver tissue', 1000000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-40'), 1, 8), '-', SUBSTR(MD5('labtest-40'), 9, 4), '-', SUBSTR(MD5('labtest-40'), 13, 4), '-', SUBSTR(MD5('labtest-40'), 17, 4), '-', SUBSTR(MD5('labtest-40'), 21, 12)), '-', '')), 'BIOPSY_BREAST', 'Breast Biopsy', 'PATHOLOGY', 'Breast tissue', 800000, NULL, 'Benign', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-41'), 1, 8), '-', SUBSTR(MD5('labtest-41'), 9, 4), '-', SUBSTR(MD5('labtest-41'), 13, 4), '-', SUBSTR(MD5('labtest-41'), 17, 4), '-', SUBSTR(MD5('labtest-41'), 21, 12)), '-', '')), 'CYTO_PAP', 'Pap Smear', 'PATHOLOGY', 'Cervical cytology', 200000, NULL, 'NILM', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-42'), 1, 8), '-', SUBSTR(MD5('labtest-42'), 9, 4), '-', SUBSTR(MD5('labtest-42'), 13, 4), '-', SUBSTR(MD5('labtest-42'), 17, 4), '-', SUBSTR(MD5('labtest-42'), 21, 12)), '-', '')), 'CYTO_URINE', 'Urine Cytology', 'PATHOLOGY', 'Urine cells', 250000, NULL, 'Negative', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-43'), 1, 8), '-', SUBSTR(MD5('labtest-43'), 9, 4), '-', SUBSTR(MD5('labtest-43'), 13, 4), '-', SUBSTR(MD5('labtest-43'), 17, 4), '-', SUBSTR(MD5('labtest-43'), 21, 12)), '-', '')), 'FNAC', 'Fine Needle Aspiration', 'PATHOLOGY', 'Cytology', 400000, NULL, 'Benign', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-44'), 1, 8), '-', SUBSTR(MD5('labtest-44'), 9, 4), '-', SUBSTR(MD5('labtest-44'), 13, 4), '-', SUBSTR(MD5('labtest-44'), 17, 4), '-', SUBSTR(MD5('labtest-44'), 21, 12)), '-', '')), 'IHC', 'Immunohistochemistry', 'PATHOLOGY', 'Tissue markers', 1200000, NULL, 'varies', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-45'), 1, 8), '-', SUBSTR(MD5('labtest-45'), 9, 4), '-', SUBSTR(MD5('labtest-45'), 13, 4), '-', SUBSTR(MD5('labtest-45'), 17, 4), '-', SUBSTR(MD5('labtest-45'), 21, 12)), '-', '')), 'FROZEN', 'Frozen Section', 'PATHOLOGY', 'Intraop pathology', 600000, NULL, 'varies', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-46'), 1, 8), '-', SUBSTR(MD5('labtest-46'), 9, 4), '-', SUBSTR(MD5('labtest-46'), 13, 4), '-', SUBSTR(MD5('labtest-46'), 17, 4), '-', SUBSTR(MD5('labtest-46'), 21, 12)), '-', '')), 'SPERM', 'Semen Analysis', 'PATHOLOGY', 'Sperm analysis', 300000, 'million/mL', '>15', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-47'), 1, 8), '-', SUBSTR(MD5('labtest-47'), 9, 4), '-', SUBSTR(MD5('labtest-47'), 13, 4), '-', SUBSTR(MD5('labtest-47'), 17, 4), '-', SUBSTR(MD5('labtest-47'), 21, 12)), '-', '')), 'STOOL', 'Stool Examination', 'PATHOLOGY', 'Fecal analysis', 100000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-48'), 1, 8), '-', SUBSTR(MD5('labtest-48'), 9, 4), '-', SUBSTR(MD5('labtest-48'), 13, 4), '-', SUBSTR(MD5('labtest-48'), 17, 4), '-', SUBSTR(MD5('labtest-48'), 21, 12)), '-', '')), 'CULTURE_BLOOD', 'Blood Culture', 'LAB', 'Bacterial culture', 300000, NULL, 'No growth', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-49'), 1, 8), '-', SUBSTR(MD5('labtest-49'), 9, 4), '-', SUBSTR(MD5('labtest-49'), 13, 4), '-', SUBSTR(MD5('labtest-49'), 17, 4), '-', SUBSTR(MD5('labtest-49'), 21, 12)), '-', '')), 'CULTURE_URINE', 'Urine Culture', 'LAB', 'Bacterial culture', 250000, 'CFU/mL', '<100000', true, NOW(), NOW(), 'system', 'system'),
    (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('labtest-50'), 1, 8), '-', SUBSTR(MD5('labtest-50'), 9, 4), '-', SUBSTR(MD5('labtest-50'), 13, 4), '-', SUBSTR(MD5('labtest-50'), 17, 4), '-', SUBSTR(MD5('labtest-50'), 21, 12)), '-', '')), 'PSA', 'Prostate Specific Antigen', 'LAB', 'Prostate marker', 280000, 'ng/mL', '<4.0', true, NOW(), NOW(), 'system', 'system');
"@

Execute-SQL -Container $SERVICES["medical-exam-service"].Container -Database $SERVICES["medical-exam-service"].Database -User $SERVICES["medical-exam-service"].User -SQL $labTestSQL
Write-Success "Medical Exam service seeded"

Write-Header "Seeding Appointment Service"
# Historical Appointments (500) via PowerShell loop for MySQL
$appointmentTypes = @('CONSULTATION', 'FOLLOW_UP', 'WALK_IN')
$reasons = @('Regular checkup', 'Follow-up visit', 'New symptoms', 'Routine exam', 'Health screening')
$departments = @('Internal Medicine', 'Cardiology', 'Orthopedics', 'Pediatrics', 'Dermatology')

$appointmentSQL = ""
for ($i = 1; $i -le 500; $i++) {
    $patNum = 1 + (($i - 1) % 1000)
    $docNum = 1 + (($i - 1) % 60)
    $type = $appointmentTypes[($i - 1) % 3]
    $reason = $reasons[($i - 1) % 5]
    $dept = $departments[($i - 1) % 5]
    $queueNum = ($i % 50) + 1
    $minOffset = $i * 30
    $appointmentSQL += "INSERT IGNORE INTO appointment (id, patient_id, patient_name, doctor_id, doctor_name, doctor_department, appointment_time, status, type, reason, notes, queue_number, priority, created_at, updated_at, created_by, updated_by) VALUES (UNHEX(REPLACE(CONCAT(SUBSTR(MD5('apt-hist-$i'), 1, 8), '-', SUBSTR(MD5('apt-hist-$i'), 9, 4), '-', SUBSTR(MD5('apt-hist-$i'), 13, 4), '-', SUBSTR(MD5('apt-hist-$i'), 17, 4), '-', SUBSTR(MD5('apt-hist-$i'), 21, 12)), '-', '')), UNHEX(REPLACE(CONCAT(SUBSTR(MD5('patient-$patNum'), 1, 8), '-', SUBSTR(MD5('patient-$patNum'), 9, 4), '-', SUBSTR(MD5('patient-$patNum'), 13, 4), '-', SUBSTR(MD5('patient-$patNum'), 17, 4), '-', SUBSTR(MD5('patient-$patNum'), 21, 12)), '-', '')), 'Patient Name $patNum', UNHEX(REPLACE(CONCAT(SUBSTR(MD5('emp-doctor-$docNum'), 1, 8), '-', SUBSTR(MD5('emp-doctor-$docNum'), 9, 4), '-', SUBSTR(MD5('emp-doctor-$docNum'), 13, 4), '-', SUBSTR(MD5('emp-doctor-$docNum'), 17, 4), '-', SUBSTR(MD5('emp-doctor-$docNum'), 21, 12)), '-', '')), 'Dr. Name $docNum', '$dept', DATE_SUB(NOW(), INTERVAL 30 DAY) + INTERVAL $minOffset MINUTE, 'COMPLETED', '$type', '$reason', 'Historical appointment for load test', $queueNum, 100, DATE_SUB(NOW(), INTERVAL 31 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), 'system', 'system');`n"
}
Execute-SQL -Container $SERVICES["appointment-service"].Container -Database $SERVICES["appointment-service"].Database -User $SERVICES["appointment-service"].User -SQL $appointmentSQL
Write-Success "Appointment service seeded"

# ============================================================================
# Final Verification
# ============================================================================

Write-Header "Final Data Verification"
$totalRecords = 0

foreach ($serviceName in $SERVICES.Keys) {
    $config = $SERVICES[$serviceName]
    Write-Info "Service: $serviceName"
    foreach ($table in $config.Tables) {
        $count = Get-TableCount -Container $config.Container -Database $config.Database -User $config.User -Table $table
        Write-Host "  - $table`: $count records"
        $totalRecords += $count
    }
}

Write-Header "Seeding Complete!"
Write-Host "Total records inserted: $totalRecords" -ForegroundColor Green
Write-Host "`nExpected totals:" -ForegroundColor Yellow
Write-Host "  - Accounts: 1155 (5 admin + 60 doctors + 50 nurses + 40 receptionists + 1000 patients)"
Write-Host "  - Patients: 1000"
Write-Host "  - Employees: 155 (60 doctors + 50 nurses + 40 receptionists + 5 admins)"
Write-Host "  - Departments: 10"
Write-Host "  - Employee Schedules: 420 (60 doctors x 7 days)"
Write-Host "  - Categories: 10"
Write-Host "  - Medicines: 200"
Write-Host "  - Lab Tests: 50"
Write-Host "  - Appointments: 500"
Write-Host "`n"
