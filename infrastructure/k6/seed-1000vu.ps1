# ============================================================================
# HMS Backend - 1000 VU Complete Seed Script (API-based)
# ============================================================================
# Creates accounts and essential records for 1000 VU load testing
#
# Accounts (1100 total):
#   - 10 Admin (admin1-10@hms.com) - Admin123!@
#   - 200 Doctor (doctor1-200@hms.com) - Doctor123!@
#   - 150 Nurse (nurse1-150@hms.com) - Nurse123!@
#   - 125 Receptionist (receptionist1-125@hms.com) - Receptionist123!@
#   - 600 Patient (patient1-600@email.com) - Patient123!@
#
# Essential Records:
#   - 6 Departments
#   - Employee profiles linked to accounts
#   - Doctor schedules (next 7 days)
#   - Patient profiles
#   - Medicine categories and medicines
#
# Usage: .\seed-1000vu.ps1 [-BaseUrl "http://localhost:8080"]
# ============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$MaxWaitSeconds = 300
)

$ErrorActionPreference = "Continue"

# Global state
$script:AdminToken = ""
$script:DeptIds = @{}
$script:DoctorEmployeeIds = @{}
$script:PatientIds = @{}
$script:CategoryIds = @{}
$script:MedicineIds = @{}

function Write-Header { param([string]$Text); Write-Host "`n=== $Text ===" -ForegroundColor Cyan }
function Write-Ok { param([string]$Text); Write-Host "[OK] $Text" -ForegroundColor Green }
function Write-Warn { param([string]$Text); Write-Host "[WARN] $Text" -ForegroundColor Yellow }
function Write-Info { param([string]$Text); Write-Host "[INFO] $Text" -ForegroundColor Blue }
function Write-Err { param([string]$Text); Write-Host "[ERR] $Text" -ForegroundColor Red }

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Endpoint,
        [object]$Body = $null,
        [string]$Token = $null,
        [string]$Desc = "",
        [bool]$Silent = $false
    )
    
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    
    $uri = "$BaseUrl$Endpoint"
    
    try {
        $params = @{ Uri = $uri; Method = $Method; Headers = $headers; TimeoutSec = 30 }
        if ($Body -and $Method -ne "GET") {
            $params["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress)
        }
        $response = Invoke-RestMethod @params
        if ($Desc -and -not $Silent) { Write-Ok $Desc }
        return $response
    }
    catch {
        if ($Desc -and -not $Silent) { Write-Warn "$Desc (failed/exists)" }
        return $null
    }
}

# ============================================================================
# Wait for services
# ============================================================================
Write-Header "HMS Backend - 1000 VU Complete Seed Script"
Write-Info "Base URL: $BaseUrl"
Write-Info "Waiting for API Gateway..."

$attempt = 0
$maxAttempts = $MaxWaitSeconds / 2
while ($attempt -lt $maxAttempts) {
    try {
        $body = @{ email = "admin@hms.com"; password = "Admin123!@" }
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -Body ($body | ConvertTo-Json) -ContentType "application/json" -TimeoutSec 5 -ErrorAction Stop
        if ($response.data.accessToken) {
            $script:AdminToken = $response.data.accessToken
            Write-Ok "Services ready! Admin logged in."
            break
        }
    }
    catch {
        $attempt++
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 2
    }
}

if (-not $script:AdminToken) {
    Write-Host ""
    Write-Err "Could not login as admin. Make sure services are running."
    exit 1
}

# ============================================================================
# Step 1: Create Departments
# ============================================================================
Write-Header "Step 1: Create Departments"

$departments = @(
    @{ name = "Cardiology"; description = "Heart and cardiovascular care"; location = "Building A, Floor 2"; phoneExtension = "1001"; status = "ACTIVE" },
    @{ name = "Neurology"; description = "Brain and nervous system"; location = "Building A, Floor 3"; phoneExtension = "1002"; status = "ACTIVE" },
    @{ name = "Pediatrics"; description = "Child healthcare"; location = "Building B, Floor 1"; phoneExtension = "1003"; status = "ACTIVE" },
    @{ name = "General Medicine"; description = "Primary care"; location = "Building A, Floor 1"; phoneExtension = "1004"; status = "ACTIVE" },
    @{ name = "Orthopedics"; description = "Bone and joint treatment"; location = "Building B, Floor 2"; phoneExtension = "1005"; status = "ACTIVE" },
    @{ name = "Dermatology"; description = "Skin conditions"; location = "Building C, Floor 1"; phoneExtension = "1006"; status = "ACTIVE" }
)

foreach ($dept in $departments) {
    $response = Invoke-Api -Method POST -Endpoint "/api/hr/departments" -Body $dept -Token $script:AdminToken -Desc $dept.name
    if ($response -and $response.data -and $response.data.id) {
        $script:DeptIds[$dept.name] = $response.data.id
    }
}

# Fetch existing if needed
if ($script:DeptIds.Count -lt $departments.Count) {
    Write-Info "Fetching existing departments..."
    $response = Invoke-Api -Method GET -Endpoint "/api/hr/departments/all?page=0&size=20" -Token $script:AdminToken
    if ($response -and $response.data -and $response.data.content) {
        foreach ($dept in $response.data.content) {
            $script:DeptIds[$dept.name] = $dept.id
        }
    }
}
Write-Info "Departments: $($script:DeptIds.Count)"

# ============================================================================
# Step 2: Create Doctor Accounts & Employees (200)
# ============================================================================
Write-Header "Step 2: Create Doctor Accounts & Employees (200)"

$deptNames = @($script:DeptIds.Keys)
$specializations = @("Cardiologist", "Neurologist", "Pediatrician", "General Physician", "Orthopedic Surgeon", "Dermatologist")

for ($i = 1; $i -le 200; $i++) {
    $accountBody = @{ email = "doctor${i}@hms.com"; password = "Doctor123!@"; role = "DOCTOR" }
    $accountResponse = Invoke-Api -Method POST -Endpoint "/api/auth/accounts" -Body $accountBody -Token $script:AdminToken -Silent $true
    $accountId = if ($accountResponse -and $accountResponse.data) { $accountResponse.data.id } else { $null }
    
    if (-not $accountId) {
        $loginBody = @{ email = "doctor${i}@hms.com"; password = "Doctor123!@" }
        $loginResponse = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody -Silent $true
        if ($loginResponse -and $loginResponse.data -and $loginResponse.data.account) { 
            $accountId = $loginResponse.data.account.id 
        }
    }
    
    $deptIndex = ($i - 1) % $deptNames.Count
    $deptName = $deptNames[$deptIndex]
    $deptId = $script:DeptIds[$deptName]
    
    $employeeBody = @{
        accountId = $accountId
        departmentId = $deptId
        fullName = "Dr. Nguyen Van $i"
        role = "DOCTOR"
        email = "doctor${i}@hms.com"
        phoneNumber = "0901" + $i.ToString("D6")
        specialization = $specializations[$deptIndex]
        licenseNumber = "MD-" + $i.ToString("D5")
        status = "ACTIVE"
    }
    
    $empResponse = Invoke-Api -Method POST -Endpoint "/api/hr/employees" -Body $employeeBody -Token $script:AdminToken -Silent $true
    if ($empResponse -and $empResponse.data -and $empResponse.data.id) {
        $script:DoctorEmployeeIds["doctor$i"] = $empResponse.data.id
    }
    
    if ($i % 50 -eq 0) { Write-Ok "Created $i doctors" }
}
Write-Info "Doctor employees: $($script:DoctorEmployeeIds.Count)"

# ============================================================================
# Step 3: Create Nurse Accounts & Employees (150)
# ============================================================================
Write-Header "Step 3: Create Nurse Accounts & Employees (150)"

for ($i = 1; $i -le 150; $i++) {
    $accountBody = @{ email = "nurse${i}@hms.com"; password = "Nurse123!@"; role = "NURSE" }
    $accountResponse = Invoke-Api -Method POST -Endpoint "/api/auth/accounts" -Body $accountBody -Token $script:AdminToken -Silent $true
    $accountId = if ($accountResponse -and $accountResponse.data) { $accountResponse.data.id } else { $null }
    
    if (-not $accountId) {
        $loginBody = @{ email = "nurse${i}@hms.com"; password = "Nurse123!@" }
        $loginResponse = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody -Silent $true
        if ($loginResponse -and $loginResponse.data -and $loginResponse.data.account) { 
            $accountId = $loginResponse.data.account.id 
        }
    }
    
    $deptIndex = ($i - 1) % $deptNames.Count
    $deptId = $script:DeptIds[$deptNames[$deptIndex]]
    
    $employeeBody = @{
        accountId = $accountId
        departmentId = $deptId
        fullName = "Nurse Tran Thi $i"
        role = "NURSE"
        email = "nurse${i}@hms.com"
        phoneNumber = "0912" + $i.ToString("D6")
        status = "ACTIVE"
    }
    Invoke-Api -Method POST -Endpoint "/api/hr/employees" -Body $employeeBody -Token $script:AdminToken -Silent $true | Out-Null
    
    if ($i % 50 -eq 0) { Write-Ok "Created $i nurses" }
}
Write-Ok "Created 150 nurses"

# ============================================================================
# Step 4: Create Receptionist Accounts & Employees (125)
# ============================================================================
Write-Header "Step 4: Create Receptionist Accounts & Employees (125)"

for ($i = 1; $i -le 125; $i++) {
    $accountBody = @{ email = "receptionist${i}@hms.com"; password = "Receptionist123!@"; role = "RECEPTIONIST" }
    $accountResponse = Invoke-Api -Method POST -Endpoint "/api/auth/accounts" -Body $accountBody -Token $script:AdminToken -Silent $true
    $accountId = if ($accountResponse -and $accountResponse.data) { $accountResponse.data.id } else { $null }
    
    if (-not $accountId) {
        $loginBody = @{ email = "receptionist${i}@hms.com"; password = "Receptionist123!@" }
        $loginResponse = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody -Silent $true
        if ($loginResponse -and $loginResponse.data -and $loginResponse.data.account) { 
            $accountId = $loginResponse.data.account.id 
        }
    }
    
    $deptIndex = ($i - 1) % $deptNames.Count
    $deptId = $script:DeptIds[$deptNames[$deptIndex]]
    
    $employeeBody = @{
        accountId = $accountId
        departmentId = $deptId
        fullName = "Receptionist Le Kim $i"
        role = "RECEPTIONIST"
        email = "receptionist${i}@hms.com"
        phoneNumber = "0923" + $i.ToString("D6")
        status = "ACTIVE"
    }
    Invoke-Api -Method POST -Endpoint "/api/hr/employees" -Body $employeeBody -Token $script:AdminToken -Silent $true | Out-Null
    
    if ($i % 50 -eq 0) { Write-Ok "Created $i receptionists" }
}
Write-Ok "Created 125 receptionists"

# ============================================================================
# Step 5: Create Admin Accounts (10)
# ============================================================================
Write-Header "Step 5: Create Admin Accounts (10)"

for ($i = 1; $i -le 10; $i++) {
    $accountBody = @{ email = "admin${i}@hms.com"; password = "Admin123!@"; role = "ADMIN" }
    Invoke-Api -Method POST -Endpoint "/api/auth/accounts" -Body $accountBody -Token $script:AdminToken -Desc "admin${i}@hms.com" | Out-Null
}

# ============================================================================
# Step 6: Create Doctor Schedules (next 7 days)
# ============================================================================
Write-Header "Step 6: Create Doctor Schedules"

# Fetch doctor employees if not already
if ($script:DoctorEmployeeIds.Count -lt 200) {
    Write-Info "Fetching existing doctor employees..."
    $response = Invoke-Api -Method GET -Endpoint "/api/hr/employees/all?filter=role==DOCTOR&page=0&size=250" -Token $script:AdminToken
    if ($response -and $response.data -and $response.data.content) {
        $idx = 1
        foreach ($emp in $response.data.content) {
            $script:DoctorEmployeeIds["doctor$idx"] = $emp.id
            $idx++
        }
    }
}

$scheduleCount = 0
foreach ($doctorKey in $script:DoctorEmployeeIds.Keys) {
    $doctorId = $script:DoctorEmployeeIds[$doctorKey]
    for ($day = 1; $day -le 7; $day++) {
        $workDate = (Get-Date).AddDays($day).ToString("yyyy-MM-dd")
        $schedule = @{
            employeeId = $doctorId
            workDate = $workDate
            startTime = "09:00:00"
            endTime = "17:00:00"
            status = "AVAILABLE"
        }
        Invoke-Api -Method POST -Endpoint "/api/hr/schedules" -Body $schedule -Token $script:AdminToken -Silent $true | Out-Null
        $scheduleCount++
    }
}
Write-Ok "Created $scheduleCount schedules ($($script:DoctorEmployeeIds.Count) doctors x 7 days)"

# ============================================================================
# Step 7: Create Patient Accounts & Profiles (600)
# ============================================================================
Write-Header "Step 7: Create Patient Accounts & Profiles (600)"

$genders = @("MALE", "FEMALE")
$bloodTypes = @("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

for ($i = 1; $i -le 600; $i++) {
    $accountBody = @{ email = "patient${i}@email.com"; password = "Patient123!@" }
    $accountResponse = Invoke-Api -Method POST -Endpoint "/api/auth/register" -Body $accountBody -Silent $true
    $accountId = if ($accountResponse -and $accountResponse.data) { $accountResponse.data.id } else { $null }
    
    if (-not $accountId) {
        $loginBody = @{ email = "patient${i}@email.com"; password = "Patient123!@" }
        $loginResponse = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody -Silent $true
        if ($loginResponse -and $loginResponse.data -and $loginResponse.data.account) { 
            $accountId = $loginResponse.data.account.id 
        }
    }
    
    $year = 1970 + ($i % 40)
    $month = (($i % 12) + 1).ToString("D2")
    $day = (($i % 28) + 1).ToString("D2")
    $dateOfBirth = "${year}-${month}-${day}"
    
    $patientBody = @{
        fullName = "Patient Nguyen Van $i"
        email = "patient${i}@email.com"
        dateOfBirth = $dateOfBirth
        gender = $genders[$i % 2]
        phoneNumber = "09" + $i.ToString("D8")
        address = "$i Main Street, HCMC"
        bloodType = $bloodTypes[$i % 8]
    }
    if ($accountId) { $patientBody["accountId"] = $accountId }
    
    $patientResponse = Invoke-Api -Method POST -Endpoint "/api/patients" -Body $patientBody -Token $script:AdminToken -Silent $true
    if ($patientResponse -and $patientResponse.data) {
        $script:PatientIds["patient$i"] = $patientResponse.data.id
    }
    
    if ($i % 100 -eq 0) { Write-Ok "Created $i patients" }
}
Write-Info "Patient profiles: $($script:PatientIds.Count)"

# Verify patient accounts
Write-Info "Verifying patient email accounts..."
$null = docker exec mysql-auth-service mysql -u myuser -psecret auth_db -e "UPDATE accounts SET email_verified = 1 WHERE email LIKE 'patient%@email.com';" 2>&1
Write-Ok "Patient accounts email verified"

# ============================================================================
# Step 8: Create Medicine Categories & Medicines
# ============================================================================
Write-Header "Step 8: Create Medicines"

$categories = @(
    @{ name = "Cardiovascular"; description = "Heart medications" },
    @{ name = "Pain Relief"; description = "Analgesics and antipyretics" },
    @{ name = "Antibiotics"; description = "Bacterial infection treatment" },
    @{ name = "Respiratory"; description = "Lung and breathing medications" },
    @{ name = "Vitamins"; description = "Nutritional supplements" }
)

foreach ($cat in $categories) {
    $response = Invoke-Api -Method POST -Endpoint "/api/medicines/categories" -Body $cat -Token $script:AdminToken -Desc $cat.name
    if ($response -and $response.data) {
        $script:CategoryIds[$cat.name] = $response.data.id
    }
}

# Fetch existing categories if needed
if ($script:CategoryIds.Count -eq 0) {
    $response = Invoke-Api -Method GET -Endpoint "/api/medicines/categories?all=true" -Token $script:AdminToken
    if ($response -and $response.data) {
        foreach ($cat in $response.data) {
            $script:CategoryIds[$cat.name] = $cat.id
        }
    }
}

# Create medicines with higher quantities
$categoryId = $script:CategoryIds.Values | Select-Object -First 1
if ($categoryId) {
    $medicines = @(
        @{ name = "Aspirin 100mg"; activeIngredient = "Aspirin"; unit = "Tablet"; quantity = 50000; purchasePrice = 10000; sellingPrice = 15000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "PharmaCorp"; categoryId = $categoryId },
        @{ name = "Lisinopril 10mg"; activeIngredient = "Lisinopril"; unit = "Tablet"; quantity = 30000; purchasePrice = 50000; sellingPrice = 65000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "CardioPharm"; categoryId = $categoryId },
        @{ name = "Amoxicillin 500mg"; activeIngredient = "Amoxicillin"; unit = "Capsule"; quantity = 40000; purchasePrice = 30000; sellingPrice = 45000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "MediCo"; categoryId = $categoryId },
        @{ name = "Paracetamol 500mg"; activeIngredient = "Paracetamol"; unit = "Tablet"; quantity = 100000; purchasePrice = 5000; sellingPrice = 8000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "GenericPharma"; categoryId = $categoryId },
        @{ name = "Ibuprofen 400mg"; activeIngredient = "Ibuprofen"; unit = "Tablet"; quantity = 60000; purchasePrice = 15000; sellingPrice = 22000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "PainRelief Inc"; categoryId = $categoryId },
        @{ name = "Metformin 500mg"; activeIngredient = "Metformin"; unit = "Tablet"; quantity = 40000; purchasePrice = 20000; sellingPrice = 30000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "DiabCare"; categoryId = $categoryId },
        @{ name = "Omeprazole 20mg"; activeIngredient = "Omeprazole"; unit = "Capsule"; quantity = 30000; purchasePrice = 25000; sellingPrice = 35000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "GastroMed"; categoryId = $categoryId },
        @{ name = "Vitamin C 1000mg"; activeIngredient = "Ascorbic Acid"; unit = "Tablet"; quantity = 80000; purchasePrice = 8000; sellingPrice = 12000; expiresAt = "2027-12-31T00:00:00Z"; manufacturer = "VitaCorp"; categoryId = $categoryId }
    )
    
    foreach ($med in $medicines) {
        $response = Invoke-Api -Method POST -Endpoint "/api/medicines" -Body $med -Token $script:AdminToken -Desc $med.name
        if ($response -and $response.data) {
            $script:MedicineIds[$med.name] = $response.data.id
        }
    }
}
Write-Info "Medicines: $($script:MedicineIds.Count)"

# ============================================================================
# Verification
# ============================================================================
Write-Header "Verification - Testing Logins"

$testAccounts = @(
    @{ email = "admin1@hms.com"; password = "Admin123!@"; role = "ADMIN" },
    @{ email = "doctor1@hms.com"; password = "Doctor123!@"; role = "DOCTOR" },
    @{ email = "doctor200@hms.com"; password = "Doctor123!@"; role = "DOCTOR" },
    @{ email = "nurse1@hms.com"; password = "Nurse123!@"; role = "NURSE" },
    @{ email = "nurse150@hms.com"; password = "Nurse123!@"; role = "NURSE" },
    @{ email = "receptionist1@hms.com"; password = "Receptionist123!@"; role = "RECEPTIONIST" },
    @{ email = "receptionist125@hms.com"; password = "Receptionist123!@"; role = "RECEPTIONIST" },
    @{ email = "patient1@email.com"; password = "Patient123!@"; role = "PATIENT" },
    @{ email = "patient600@email.com"; password = "Patient123!@"; role = "PATIENT" }
)

$allPassed = $true
foreach ($account in $testAccounts) {
    $body = @{ email = $account.email; password = $account.password }
    $response = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $body -Silent $true
    if ($response -and $response.code -eq 1000) {
        Write-Ok "$($account.role): $($account.email)"
    } else {
        Write-Warn "$($account.role): $($account.email) - LOGIN FAILED"
        $allPassed = $false
    }
}

# ============================================================================
# Summary
# ============================================================================
Write-Header "Summary"
Write-Host "Created:"
Write-Host "  - Departments: $($script:DeptIds.Count)"
Write-Host "  - Doctor employees: ~200"
Write-Host "  - Nurse employees: ~150"
Write-Host "  - Receptionist employees: ~125"
Write-Host "  - Doctor schedules: ~1400 (200 doctors x 7 days)"
Write-Host "  - Patient profiles: $($script:PatientIds.Count)"
Write-Host "  - Medicine categories: $($script:CategoryIds.Count)"
Write-Host "  - Medicines: $($script:MedicineIds.Count)"
Write-Host ""
Write-Host "Accounts (1100 total):" -ForegroundColor Yellow
Write-Host "  - 10 Admin (admin1-10@hms.com) - Admin123!@"
Write-Host "  - 200 Doctor (doctor1-200@hms.com) - Doctor123!@"
Write-Host "  - 150 Nurse (nurse1-150@hms.com) - Nurse123!@"
Write-Host "  - 125 Receptionist (receptionist1-125@hms.com) - Receptionist123!@"
Write-Host "  - 600 Patient (patient1-600@email.com) - Patient123!@"
Write-Host ""

if ($allPassed) {
    Write-Host "All login tests PASSED! Ready for k6 load testing with 500-1000 VUs." -ForegroundColor Green
} else {
    Write-Host "Some logins failed. Check services." -ForegroundColor Yellow
}
