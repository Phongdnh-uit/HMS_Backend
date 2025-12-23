# ============================================================================
# HMS Backend - Notification Test Data Seeder
# Sets followUpDate = TODAY on an existing COMPLETED appointment
# ============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$TestEmail = "nhuocphu13@gmail.com"
)

$ErrorActionPreference = "Continue"

function Write-Ok { param([string]$Text) Write-Host "  [OK] $Text" -ForegroundColor Green }
function Write-Warn { param([string]$Text) Write-Host "  [WARN] $Text" -ForegroundColor Yellow }
function Write-Msg { param([string]$Text) Write-Host "  [INFO] $Text" -ForegroundColor Blue }

function Invoke-Api {
    param([string]$Method, [string]$Endpoint, [object]$Body = $null, [string]$Token = $null, [string]$Description = "")
    
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    
    try {
        $params = @{ Uri = "$BaseUrl$Endpoint"; Method = $Method; Headers = $headers; TimeoutSec = 30 }
        if ($Body -and $Method -ne "GET") { $params["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress) }
        $response = Invoke-RestMethod @params
        if ($Description) { Write-Ok $Description }
        return $response
    }
    catch {
        $statusCode = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 }
        if ($Description) { Write-Warn "$Description (HTTP $statusCode)" }
        return $null
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Notification Test - Quick Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Msg "Test Email: $TestEmail"
Write-Host ""

# Step 1: Login as Admin
Write-Msg "Logging in as admin..."
$loginBody = @{ email = "admin@hms.com"; password = "Admin123!@" }
$response = Invoke-Api -Method POST -Endpoint "/api/auth/login" -Body $loginBody -Description "Admin login"
if (-not $response -or -not $response.data) {
    Write-Host "Failed to login as admin. Make sure services are running." -ForegroundColor Red
    exit 1
}
$adminToken = $response.data.accessToken

# Step 2: Find test patient
Write-Msg "Finding patient with email $TestEmail..."
$response = Invoke-Api -Method GET -Endpoint "/api/patients/all?filter=email=='$TestEmail'" -Token $adminToken
$patientId = $null
$patientName = ""

if ($response -and $response.data -and $response.data.content -and $response.data.content.Count -gt 0) {
    $patientId = $response.data.content[0].id
    $patientName = $response.data.content[0].fullName
    Write-Ok "Found patient: $patientName ($patientId)"
} else {
    Write-Host "Patient with email $TestEmail not found." -ForegroundColor Red
    Write-Host "Create a patient with this email first (via Admin UI or seed-data.ps1)" -ForegroundColor Yellow
    exit 1
}

# Step 3: Find any COMPLETED appointment for this patient
Write-Msg "Finding COMPLETED appointment for patient..."
$response = Invoke-Api -Method GET -Endpoint "/api/appointments/all?filter=patientId=='$patientId';status==COMPLETED&page=0&size=1" -Token $adminToken

$appointmentId = $null
if ($response -and $response.data -and $response.data.content -and $response.data.content.Count -gt 0) {
    $appointment = $response.data.content[0]
    $appointmentId = $appointment.id
    Write-Ok "Found COMPLETED appointment: $appointmentId"
} else {
    # Try to find any appointment
    Write-Msg "No COMPLETED appointment found, looking for any appointment..."
    $response = Invoke-Api -Method GET -Endpoint "/api/appointments/all?filter=patientId=='$patientId'&page=0&size=1" -Token $adminToken
    if ($response -and $response.data -and $response.data.content -and $response.data.content.Count -gt 0) {
        $appointment = $response.data.content[0]
        $appointmentId = $appointment.id
        Write-Msg "Found appointment: $appointmentId (status: $($appointment.status))"
        
        # Complete it if not completed
        if ($appointment.status -ne "COMPLETED") {
            Write-Msg "Completing appointment..."
            Invoke-Api -Method PATCH -Endpoint "/api/appointments/$appointmentId/complete" -Token $adminToken -Description "Complete appointment" | Out-Null
        }
    }
}

if (-not $appointmentId) {
    Write-Host "No appointment found for this patient." -ForegroundColor Red
    Write-Host "Please book an appointment for this patient first via the frontend." -ForegroundColor Yellow
    exit 1
}

# Step 4: Update appointment with followUpDate = TODAY using PUT
$today = (Get-Date).ToString("yyyy-MM-dd")
Write-Msg "Setting followUpDate = TODAY ($today)..."

# Get the full appointment data first
$response = Invoke-Api -Method GET -Endpoint "/api/appointments/$appointmentId" -Token $adminToken
if (-not $response -or -not $response.data) {
    Write-Host "Failed to fetch appointment details" -ForegroundColor Red
    exit 1
}

$apt = $response.data
$updateBody = @{
    patientId = $apt.patientId
    doctorId = $apt.doctorId
    appointmentTime = $apt.appointmentTime
    type = $apt.type
    reason = $apt.reason
    followUpDate = $today
}

$response = Invoke-Api -Method PUT -Endpoint "/api/appointments/$appointmentId" -Body $updateBody -Token $adminToken -Description "Set followUpDate"

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  SUCCESS!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Patient: $patientName" -ForegroundColor Yellow
Write-Host "  Patient Email: $TestEmail" -ForegroundColor Yellow
Write-Host "  Appointment ID: $appointmentId"
Write-Host "  Follow-up Date: $today (TODAY)"
Write-Host ""
Write-Host "  The scheduler (every 20 sec with your config) should:" -ForegroundColor Cyan
Write-Host "  1. Find this appointment" -ForegroundColor White
Write-Host "  2. Send email to: $TestEmail" -ForegroundColor White
Write-Host ""
Write-Host "  Check notification-service logs for:" -ForegroundColor Yellow
Write-Host "    'Found 1 appointments needing follow-up notification'" -ForegroundColor White
Write-Host ""
