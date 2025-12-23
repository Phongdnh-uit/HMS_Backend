# ============================================================================
# HMS Backend - Data Seeding Script (PowerShell)
# ============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$MaxWaitSeconds = 120
)

$ErrorActionPreference = "Continue"

# ============================================================================
# Helper Functions
# ============================================================================

function Write-Header {
    param([string]$Text)
    Write-Host ""
    Write-Host ("=" * 60) -ForegroundColor Cyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host ("=" * 60) -ForegroundColor Cyan
    Write-Host ""
}

function Write-Ok {
    param([string]$Text)
    Write-Host "  [OK] $Text" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Text)
    Write-Host "  [WARN] $Text" -ForegroundColor Yellow
}

function Write-Err {
    param([string]$Text)
    Write-Host "  [ERR] $Text" -ForegroundColor Red
}

function Write-Msg {
    param([string]$Text)
    Write-Host "  [INFO] $Text" -ForegroundColor Blue
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Endpoint,
        [object]$Body = $null,
        [string]$Token = $null,
        [string]$Description = ""
    )
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    
    $uri = "$BaseUrl$Endpoint"
    
    try {
        $params = @{
            Uri = $uri
            Method = $Method
            Headers = $headers
            TimeoutSec = 30
        }
        
        if ($Body -and $Method -ne "GET") {
            $jsonBody = $Body | ConvertTo-Json -Depth 10 -Compress
            $params["Body"] = $jsonBody
        }
        
        $response = Invoke-RestMethod @params
        
        if ($Description) {
            Write-Ok $Description
        }
        
        return $response
    }
    catch {
        $statusCode = 0
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($Description) {
            Write-Warn "$Description (HTTP $statusCode)"
        }
        return $null
    }
}

function Wait-ForServices {
    Write-Msg "Waiting for API Gateway..."
    $attempt = 0
    $maxAttempts = $MaxWaitSeconds / 2
    
    while ($attempt -lt $maxAttempts) {
        try {
            $body = @{ email = "admin@hms.com"; password = "Admin123!@" }
            $response = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -Body ($body | ConvertTo-Json) -ContentType "application/json" -TimeoutSec 5 -ErrorAction Stop
            Write-Ok "Services ready!"
            return $response
        }
        catch {
            $attempt++
            Write-Host "." -NoNewline
            Start-Sleep -Seconds 2
        }
    }
    
    Write-Err "Services not ready"
    exit 1
}

# ============================================================================
# Global Variables
# ============================================================================
$script:AdminToken = ""
$script:DoctorToken = ""
$script:PatientToken = ""
$script:DeptIds = @{}
$script:DoctorIds = @{}
$script:PatientIds = @{}
$script:MedicineIds = @{}
$script:CategoryId = ""
$script:AppointmentIds = @{}
$script:ExamIds = @{}
$script:PrescriptionIds = @{}

# ============================================================================
# Step 1: Admin Login
# ============================================================================
function Seed-AdminLogin {
    Write-Header "Step 1: Admin Login"
    
    $body = @{
        email = "admin@hms.com"
        password = "Admin123!@"
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $body -Description "Admin login"
    
    if ($response -and $response.data) {
        $script:AdminToken = $response.data.accessToken
        Write-Msg "Token obtained"
    } else {
        Write-Err "Failed to login"
        exit 1
    }
}

# ============================================================================
# Step 2: Create Departments
# ============================================================================
function Seed-Departments {
    Write-Header "Step 2: Create Departments"
    
    $departments = @(
        @{ name = "Cardiology"; description = "Heart care"; location = "Building A, Floor 2"; phoneExtension = "1001"; status = "ACTIVE" },
        @{ name = "Neurology"; description = "Brain care"; location = "Building A, Floor 3"; phoneExtension = "1002"; status = "ACTIVE" },
        @{ name = "Pediatrics"; description = "Child care"; location = "Building B, Floor 1"; phoneExtension = "1003"; status = "ACTIVE" },
        @{ name = "General Medicine"; description = "Primary care"; location = "Building A, Floor 1"; phoneExtension = "1004"; status = "ACTIVE" }
    )
    
    foreach ($dept in $departments) {
        $response = Invoke-Api -Method POST -Endpoint "/api/hr/departments" -Body $dept -Token $script:AdminToken -Description "Create $($dept.name)"
        if ($response -and $response.data -and $response.data.id) {
            $script:DeptIds[$dept.name] = $response.data.id
        }
    }
    
    # Fetch if creation failed (already exists)
    if ($script:DeptIds.Count -lt $departments.Count) {
        Write-Msg "Fetching existing departments..."
        $response = Invoke-Api -Method GET -Endpoint "/api/hr/departments/all?page=0&size=20" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.content) {
            foreach ($dept in $response.data.content) {
                $script:DeptIds[$dept.name] = $dept.id
            }
        }
    }
    
    Write-Msg "Departments: $($script:DeptIds.Count)"
}

# ============================================================================
# Step 3: Create Doctor
# ============================================================================
function Seed-Doctors {
    Write-Header "Step 3: Create Doctor"
    
    $cardiologyId = $script:DeptIds["Cardiology"]
    if (-not $cardiologyId) {
        Write-Warn "No Cardiology dept found"
        # Use first available
        if ($script:DeptIds.Count -gt 0) {
            $cardiologyId = $script:DeptIds.Values | Select-Object -First 1
        }
    }
    
    if (-not $cardiologyId) {
        Write-Err "No department available"
        return
    }
    
    # Create account
    $doctorAccount = @{
        email = "doctor1@hms.com"
        password = "Doctor123!@"
        role = "DOCTOR"
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/auth/accounts" -Body $doctorAccount -Token $script:AdminToken -Description "Doctor account"
    $doctorAccountId = if ($response -and $response.data) { $response.data.id } else { $null }
    
    # Create employee
    $doctor = @{
        accountId = $doctorAccountId
        departmentId = $cardiologyId
        fullName = "Dr. John Smith"
        role = "DOCTOR"
        email = "doctor1@hms.com"
        phoneNumber = "0901234567"
        specialization = "Cardiologist"
        licenseNumber = "MD-12345"
        status = "ACTIVE"
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/hr/employees" -Body $doctor -Token $script:AdminToken -Description "Doctor employee"
    if ($response -and $response.data -and $response.data.id) {
        $script:DoctorIds["doctor1"] = $response.data.id
    }
    
    # Fetch if not created
    if (-not $script:DoctorIds["doctor1"]) {
        Write-Msg "Fetching existing doctor..."
        $response = Invoke-Api -Method GET -Endpoint "/api/hr/employees/all?filter=role==DOCTOR&page=0&size=10" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.content -and $response.data.content.Count -gt 0) {
            $script:DoctorIds["doctor1"] = $response.data.content[0].id
        }
    }
    
    # Login
    $loginBody = @{ email = "doctor1@hms.com"; password = "Doctor123!@" }
    $response = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody -Description "Doctor login"
    if ($response -and $response.data) {
        $script:DoctorToken = $response.data.accessToken
    }
    
    Write-Msg "Doctor ID: $($script:DoctorIds['doctor1'])"
}

# ============================================================================
# Step 4: Create Schedules
# ============================================================================
function Seed-Schedules {
    Write-Header "Step 4: Create Schedules"
    
    $doctorId = $script:DoctorIds["doctor1"]
    if (-not $doctorId) {
        Write-Warn "No doctor ID"
        return
    }
    
    # Create schedules for past 30 days AND next 7 days (for report data)
    for ($i = -30; $i -le 7; $i++) {
        $workDate = (Get-Date).AddDays($i).ToString("yyyy-MM-dd")
        $schedule = @{
            employeeId = $doctorId
            workDate = $workDate
            startTime = "09:00:00"
            endTime = "17:00:00"
            status = "AVAILABLE"
        }
        Invoke-Api -Method POST -Endpoint "/api/hr/schedules" -Body $schedule -Token $script:AdminToken -Description "Schedule $workDate" | Out-Null
    }
}

# ============================================================================
# Step 5: Create Patient
# ============================================================================
function Seed-Patients {
    Write-Header "Step 5: Create Patient"
    
    $patientAccount = @{
        email = "patient1@gmail.com"
        password = "Patient123!@"
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/auth/register" -Body $patientAccount -Description "Patient register"
    $patientAccountId = if ($response -and $response.data) { $response.data.id } else { $null }
    
    if (-not $patientAccountId) {
        $loginBody = @{ email = "patient1@gmail.com"; password = "Patient123!@" }
        $response = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody -Description "Patient login"
        if ($response -and $response.data) {
            $patientAccountId = $response.data.id
            $script:PatientToken = $response.data.accessToken
        }
    }
    
    $patient = @{
        accountId = $patientAccountId
        fullName = "Nguyen Van An"
        email = "patient1@gmail.com"
        dateOfBirth = "1990-01-15"
        gender = "MALE"
        phoneNumber = "0901234567"
        address = "123 Main St, HCMC"
        bloodType = "O+"
        allergies = "Penicillin"
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/patients" -Body $patient -Token $script:AdminToken -Description "Patient profile"
    if ($response -and $response.data) {
        $script:PatientIds["patient1"] = $response.data.id
    }
    
    # Fetch if not created
    if (-not $script:PatientIds["patient1"]) {
        Write-Msg "Fetching existing patient..."
        $response = Invoke-Api -Method GET -Endpoint "/api/patients?page=0&size=10" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.content -and $response.data.content.Count -gt 0) {
            $script:PatientIds["patient1"] = $response.data.content[0].id
        }
    }
    
    if (-not $script:PatientToken) {
        $loginBody = @{ email = "patient1@gmail.com"; password = "Patient123!@" }
        $response = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody
        if ($response -and $response.data) {
            $script:PatientToken = $response.data.accessToken
        }
    }
    
    Write-Msg "Patient ID: $($script:PatientIds['patient1'])"
}

# ============================================================================
# Step 6: Create Medicines
# ============================================================================
function Seed-Medicines {
    Write-Header "Step 6: Create Medicines"
    
    $category = @{
        name = "Cardiovascular"
        description = "Heart meds"
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/medicines/categories" -Body $category -Token $script:AdminToken -Description "Category"
    if ($response -and $response.data) {
        $script:CategoryId = $response.data.id
    }
    
    if (-not $script:CategoryId) {
        $response = Invoke-Api -Method GET -Endpoint "/api/medicines/categories?all=true" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.Count -gt 0) {
            $script:CategoryId = $response.data[0].id
        }
    }
    
    $medicines = @(
        @{ name = "Lisinopril 10mg"; activeIngredient = "Lisinopril"; unit = "Tablet"; quantity = 1000; purchasePrice = 50000; sellingPrice = 65000; expiresAt = "2026-12-31T00:00:00Z"; manufacturer = "CardioPharm"; categoryId = $script:CategoryId },
        @{ name = "Aspirin 81mg"; activeIngredient = "Aspirin"; unit = "Tablet"; quantity = 2000; purchasePrice = 10000; sellingPrice = 15000; expiresAt = "2026-12-31T00:00:00Z"; manufacturer = "PharmaCorp"; categoryId = $script:CategoryId }
    )
    
    foreach ($med in $medicines) {
        $response = Invoke-Api -Method POST -Endpoint "/api/medicines" -Body $med -Token $script:AdminToken -Description $med.name
        if ($response -and $response.data) {
            $script:MedicineIds[$med.name] = $response.data.id
        }
    }
    
    if ($script:MedicineIds.Count -eq 0) {
        $response = Invoke-Api -Method GET -Endpoint "/api/medicines?page=0&size=10" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.content) {
            foreach ($med in $response.data.content) {
                $script:MedicineIds[$med.name] = $med.id
            }
        }
    }
    
    Write-Msg "Medicines: $($script:MedicineIds.Count)"
}

# ============================================================================
# Step 7: Create Appointment
# ============================================================================
function Seed-Appointments {
    Write-Header "Step 7: Create Appointment"
    
    $patientId = $script:PatientIds["patient1"]
    $doctorId = $script:DoctorIds["doctor1"]
    
    if (-not $patientId -or -not $doctorId) {
        Write-Warn "Missing patient ($patientId) or doctor ($doctorId)"
        return
    }
    
    # Use tomorrow at 10:00 AM (schedule exists for future dates)
    $tomorrow = (Get-Date).Date.AddDays(1).AddHours(10)  # Tomorrow 10:00 AM local
    $appointmentTime = $tomorrow.ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    
    $appointment = @{
        patientId = $patientId
        doctorId = $doctorId
        appointmentTime = $appointmentTime
        type = "CONSULTATION"
        reason = "Chest pain"
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/appointments" -Body $appointment -Token $script:PatientToken -Description "Appointment"
    if ($response -and $response.data) {
        $script:AppointmentIds["apt1"] = $response.data.id
    }
    
    if (-not $script:AppointmentIds["apt1"]) {
        $response = Invoke-Api -Method GET -Endpoint "/api/appointments/all?page=0&size=10" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.content -and $response.data.content.Count -gt 0) {
            $script:AppointmentIds["apt1"] = $response.data.content[0].id
        }
    }
    
    $aptId = $script:AppointmentIds["apt1"]
    if ($aptId) {
        Invoke-Api -Method PATCH -Endpoint "/api/appointments/$aptId/complete" -Token $script:DoctorToken -Description "Complete apt" | Out-Null
    }
    
    Write-Msg "Appointment ID: $aptId"
}

# ============================================================================
# Step 8: Create Medical Exam
# ============================================================================
function Seed-MedicalExam {
    Write-Header "Step 8: Create Medical Exam"
    
    $aptId = $script:AppointmentIds["apt1"]
    
    if (-not $aptId) {
        Write-Warn "No appointment ID"
        return
    }
    
    $exam = @{
        appointmentId = $aptId
        diagnosis = "Hypertension Stage 1"
        symptoms = "Headache, dizziness"
        treatment = "Medication"
        temperature = 36.8
        bloodPressureSystolic = 150
        bloodPressureDiastolic = 95
        heartRate = 85
        weight = 72.5
        height = 168.0
        notes = "Prescribing medication"
        hasPrescription = $true
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/exams" -Body $exam -Token $script:DoctorToken -Description "Medical exam"
    if ($response -and $response.data) {
        $script:ExamIds["exam1"] = $response.data.id
    }
    
    if (-not $script:ExamIds["exam1"]) {
        $response = Invoke-Api -Method GET -Endpoint "/api/exams/by-appointment/$aptId" -Token $script:DoctorToken
        if ($response -and $response.data) {
            $script:ExamIds["exam1"] = $response.data.id
        }
    }
    
    Write-Msg "Exam ID: $($script:ExamIds['exam1'])"
}

# ============================================================================
# Step 9: Create Prescription & Dispense
# ============================================================================
function Seed-Prescription {
    Write-Header "Step 9: Create Prescription"
    
    $examId = $script:ExamIds["exam1"]
    
    if (-not $examId) {
        Write-Warn "No exam ID"
        return
    }
    
    $medicineId = $script:MedicineIds.Values | Select-Object -First 1
    if (-not $medicineId) {
        $response = Invoke-Api -Method GET -Endpoint "/api/medicines?page=0&size=1" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.content -and $response.data.content.Count -gt 0) {
            $medicineId = $response.data.content[0].id
        }
    }
    
    if (-not $medicineId) {
        Write-Warn "No medicine ID"
        return
    }
    
    $prescription = @{
        notes = "Follow up in 2 weeks"
        items = @(
            @{
                medicineId = $medicineId
                quantity = 30
                dosage = "10mg once daily"
                durationDays = 30
                instructions = "Take with water"
            }
        )
    }
    
    $response = Invoke-Api -Method POST -Endpoint "/api/exams/$examId/prescriptions" -Body $prescription -Token $script:DoctorToken -Description "Prescription"
    if ($response -and $response.data) {
        $script:PrescriptionIds["rx1"] = $response.data.id
    }
    
    if (-not $script:PrescriptionIds["rx1"]) {
        $response = Invoke-Api -Method GET -Endpoint "/api/exams/$examId/prescription" -Token $script:DoctorToken
        if ($response -and $response.data) {
            $script:PrescriptionIds["rx1"] = $response.data.id
        }
    }
    
    $rxId = $script:PrescriptionIds["rx1"]
    # Skip dispense - let user test dispense button in frontend
    # if ($rxId) {
    #     Invoke-Api -Method POST -Endpoint "/api/exams/prescriptions/$rxId/dispense" -Token $script:AdminToken -Description "Dispense (auto-invoice)" | Out-Null
    # }
    
    Write-Msg "Prescription ID: $rxId (ACTIVE - not dispensed)"
}

# ============================================================================
# Step 10: Seed Report Data - Multiple Patients
# ============================================================================
function Seed-ReportPatients {
    Write-Header "Step 10: Seed More Patients for Reports"
    
    $patients = @(
        @{ fullName = "Tran Thi Mai"; email = "patient2@gmail.com"; dateOfBirth = "1985-03-20"; gender = "FEMALE"; phoneNumber = "0902222222"; address = "456 Nguyen Hue, HCMC"; bloodType = "A+"; allergies = "" },
        @{ fullName = "Le Van Hung"; email = "patient3@gmail.com"; dateOfBirth = "1978-07-10"; gender = "MALE"; phoneNumber = "0903333333"; address = "789 Le Loi, HCMC"; bloodType = "B+"; allergies = "Aspirin" },
        @{ fullName = "Pham Thi Lan"; email = "patient4@gmail.com"; dateOfBirth = "1995-11-05"; gender = "FEMALE"; phoneNumber = "0904444444"; address = "321 Hai Ba Trung, HCMC"; bloodType = "AB+"; allergies = "" },
        @{ fullName = "Hoang Van Duc"; email = "patient5@gmail.com"; dateOfBirth = "1960-02-28"; gender = "MALE"; phoneNumber = "0905555555"; address = "654 Dong Khoi, HCMC"; bloodType = "O-"; allergies = "" },
        @{ fullName = "Nguyen Thi Hoa"; email = "patient6@gmail.com"; dateOfBirth = "2010-08-15"; gender = "FEMALE"; phoneNumber = "0906666666"; address = "987 CMT8, HCMC"; bloodType = "A-"; allergies = "Nuts" }
    )
    
    $index = 2
    foreach ($patient in $patients) {
        # Register account
        $accountBody = @{ email = $patient.email; password = "Patient123!@" }
        $response = Invoke-Api -Method POST -Endpoint "/api/auth/register" -Body $accountBody -Description "Register $($patient.fullName)"
        $accountId = if ($response -and $response.data) { $response.data.id } else { $null }
        
        if (-not $accountId) {
            $loginBody = @{ email = $patient.email; password = "Patient123!@" }
            $response = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody
            if ($response -and $response.data) { $accountId = $response.data.id }
        }
        
        $patient["accountId"] = $accountId
        $response = Invoke-Api -Method POST -Endpoint "/api/patients" -Body $patient -Token $script:AdminToken -Description "Patient $($patient.fullName)"
        if ($response -and $response.data) {
            $script:PatientIds["patient$index"] = $response.data.id
        }
        $index++
    }
    
    Write-Msg "Total patients: $($script:PatientIds.Count)"
}

# ============================================================================
# Step 11: Seed Report Data - Multiple Appointments
# ============================================================================
function Seed-ReportAppointments {
    Write-Header "Step 11: Seed More Appointments for Reports"
    
    $doctorId = $script:DoctorIds["doctor1"]
    if (-not $doctorId) {
        Write-Warn "No doctor ID"
        return
    }
    
    $types = @("CONSULTATION", "FOLLOW_UP", "CHECK_UP")
    $reasons = @("Regular checkup", "Follow-up visit", "Chest pain", "Headache", "Vaccination")
    
    $appointmentCount = 0
    $patientIndex = 0
    $patientKeys = @($script:PatientIds.Keys)
    
    # Spread appointments across days 1-6 (schedules exist for these future dates)
    for ($day = 1; $day -le 6; $day++) {
        for ($hour = 9; $hour -le 14; $hour++) {
            if ($patientIndex -ge $patientKeys.Count) {
                $patientIndex = 0  # Cycle back to first patient
            }
            
            $patientKey = $patientKeys[$patientIndex]
            $patientId = $script:PatientIds[$patientKey]
            
            $appointmentDate = (Get-Date).Date.AddDays($day).AddHours($hour)
            $appointmentTime = $appointmentDate.ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
            
            $appointment = @{
                patientId = $patientId
                doctorId = $doctorId
                appointmentTime = $appointmentTime
                type = $types[$appointmentCount % $types.Count]
                reason = $reasons[$appointmentCount % $reasons.Count]
            }
            
            $response = Invoke-Api -Method POST -Endpoint "/api/appointments" -Body $appointment -Token $script:AdminToken -Description "Apt day $day hour $hour"
            if ($response -and $response.data) {
                $aptId = $response.data.id
                $script:AppointmentIds["apt_${day}_${hour}"] = $aptId
                
                # Randomly complete or cancel some
                $random = Get-Random -Minimum 0 -Maximum 10
                if ($random -lt 4) {
                    # Complete 40%
                    Invoke-Api -Method PATCH -Endpoint "/api/appointments/$aptId/complete" -Token $script:DoctorToken | Out-Null
                } elseif ($random -lt 6) {
                    # Cancel 20%
                    $cancelBody = @{ reason = "Patient request" }
                    Invoke-Api -Method PATCH -Endpoint "/api/appointments/$aptId/cancel" -Body $cancelBody -Token $script:AdminToken | Out-Null
                }
                
                $appointmentCount++
            }
            $patientIndex++
        }
    }
    
    Write-Msg "Total appointments: $($script:AppointmentIds.Count)"
}

# ============================================================================
# Step 12: Create Medical Exams for Appointments (auto-generates invoices)
# ============================================================================
function Seed-ReportExamsAndInvoices {
    Write-Header "Step 12: Create Exams & Auto-Invoices"
    
    # Medical exams auto-generate invoices when hasPrescription=false
    # Creates exam for COMPLETED appointments
    $examCount = 0
    $completedApts = @()
    
    # First, get all appointments and check which are completed
    foreach ($aptKey in $script:AppointmentIds.Keys) {
        $aptId = $script:AppointmentIds[$aptKey]
        # Get appointment to check status
        $response = Invoke-Api -Method GET -Endpoint "/api/appointments/$aptId" -Token $script:AdminToken
        if ($response -and $response.data -and $response.data.status -eq "COMPLETED") {
            $completedApts += @{ key = $aptKey; id = $aptId }
        }
    }
    
    Write-Msg "Found $($completedApts.Count) COMPLETED appointments"
    
    # Create medical exams for completed appointments (auto-generates invoices)
    foreach ($apt in $completedApts) {
        $aptId = $apt.id
        $aptKey = $apt.key
        
        # Check if exam already exists
        $existingExam = Invoke-Api -Method GET -Endpoint "/api/exams/by-appointment/$aptId" -Token $script:DoctorToken
        if ($existingExam -and $existingExam.data) {
            Write-Msg "Exam already exists for $aptKey"
            $examCount++
            continue
        }
        
        $exam = @{
            appointmentId = $aptId
            diagnosis = "General checkup - healthy"
            symptoms = "Routine visit"
            treatment = "No treatment needed"
            temperature = [float](Get-Random -Minimum 36 -Maximum 38)
            bloodPressureSystolic = Get-Random -Minimum 110 -Maximum 140
            bloodPressureDiastolic = Get-Random -Minimum 70 -Maximum 90
            heartRate = Get-Random -Minimum 60 -Maximum 100
            weight = [float](Get-Random -Minimum 50 -Maximum 90)
            height = [float](Get-Random -Minimum 150 -Maximum 185)
            notes = "Auto-generated exam for $aptKey"
            hasPrescription = $false  # This triggers auto-invoice generation
        }
        
        $response = Invoke-Api -Method POST -Endpoint "/api/exams" -Body $exam -Token $script:DoctorToken -Description "Exam for $aptKey"
        if ($response -and $response.data) {
            $examCount++
        }
    }
    
    Write-Msg "Created $examCount exams (invoices auto-generated for each)"
}

# ============================================================================
# Main
# ============================================================================

Write-Header "HMS Backend - Data Seeding"
Write-Host "Base URL: $BaseUrl"

$authResponse = Wait-ForServices
if ($authResponse -and $authResponse.data) {
    $script:AdminToken = $authResponse.data.accessToken
    Write-Msg "Admin token from health check"
} else {
    Seed-AdminLogin
}

Seed-Departments
Seed-Doctors
Seed-Schedules
Seed-Patients
Seed-Medicines
Seed-Appointments
Seed-MedicalExam
Seed-Prescription

# Report Data Seeding
Seed-ReportPatients
Seed-ReportAppointments
Seed-ReportExamsAndInvoices

Write-Header "Summary"
Write-Host "  Departments: $($script:DeptIds.Count)"
Write-Host "  Doctors: $($script:DoctorIds.Count)"
Write-Host "  Patients: $($script:PatientIds.Count)"
Write-Host "  Medicines: $($script:MedicineIds.Count)"
Write-Host "  Appointments: $($script:AppointmentIds.Count)"
Write-Host "  Exams: $($script:ExamIds.Count)"
Write-Host "  Prescriptions: $($script:PrescriptionIds.Count)"
Write-Host ""
Write-Host "Test Accounts:" -ForegroundColor Yellow
Write-Host "  Admin: admin@hms.com / Admin123!@"
Write-Host "  Doctor: doctor1@hms.com / Doctor123!@"
Write-Host "  Patient: patient1@gmail.com / Patient123!@"
Write-Host ""
Write-Host "Report Endpoints:" -ForegroundColor Yellow
Write-Host "  GET /api/reports/revenue?startDate=2024-01-01&endDate=2024-12-31"
Write-Host "  GET /api/reports/appointments?startDate=2024-01-01&endDate=2024-12-31"
Write-Host "  GET /api/reports/patients"

