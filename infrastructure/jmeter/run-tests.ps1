# HMS Load Test Runner
# Run this script from the jmeter directory

param(
    [string]$TestPlan = "HMS_LoadTest_50VU",
    [string]$Host = "localhost",
    [int]$Port = 8080,
    [switch]$GUI,
    [switch]$GenerateReport,
    [int]$Duration = 300
)

$ErrorActionPreference = "Stop"

# Check if JMeter is installed
$jmeterPath = $null
$possiblePaths = @(
    "jmeter",
    "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat",
    "C:\Users\giang\AppData\Local\Programs\JMeter\bin\jmeter.bat",
    "C:\apache-jmeter-5.6.3\bin\jmeter.bat",
    "C:\apache-jmeter\bin\jmeter.bat",
    "$env:JMETER_HOME\bin\jmeter.bat"
)

foreach ($path in $possiblePaths) {
    if (Get-Command $path -ErrorAction SilentlyContinue) {
        $jmeterPath = $path
        break
    }
    if (Test-Path $path) {
        $jmeterPath = $path
        break
    }
}

if (-not $jmeterPath) {
    Write-Host "ERROR: JMeter not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install JMeter using one of these methods:" -ForegroundColor Yellow
    Write-Host "1. Download from: https://jmeter.apache.org/download_jmeter.cgi"
    Write-Host "2. Using Chocolatey: choco install jmeter"
    Write-Host "3. Using Scoop: scoop install jmeter"
    Write-Host ""
    Write-Host "After installation, set JMETER_HOME environment variable or add JMeter to PATH"
    exit 1
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  HMS Load Test Runner" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "JMeter found at: $jmeterPath" -ForegroundColor Green
Write-Host "Test Plan: $TestPlan" -ForegroundColor Yellow
Write-Host "Target: http://${Host}:${Port}" -ForegroundColor Yellow
Write-Host "Duration: ${Duration}s" -ForegroundColor Yellow
Write-Host ""

# Get script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$testPlanPath = Join-Path $scriptDir "test-plans\$TestPlan.jmx"
$dataDir = Join-Path $scriptDir "data"
$resultsDir = Join-Path $scriptDir "results"

# Verify test plan exists
if (-not (Test-Path $testPlanPath)) {
    Write-Host "ERROR: Test plan not found: $testPlanPath" -ForegroundColor Red
    exit 1
}

# Create results directory
if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

# Generate timestamp for results
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$resultsFile = Join-Path $resultsDir "results_${TestPlan}_${timestamp}.jtl"
$reportDir = Join-Path $resultsDir "report_${TestPlan}_${timestamp}"

Write-Host "Test plan: $testPlanPath" -ForegroundColor Gray
Write-Host "Data dir: $dataDir" -ForegroundColor Gray
Write-Host "Results: $resultsFile" -ForegroundColor Gray
Write-Host ""

if ($GUI) {
    # Run in GUI mode (for debugging)
    Write-Host "Starting JMeter in GUI mode..." -ForegroundColor Yellow
    Write-Host "TIP: Click the green play button to start the test" -ForegroundColor Cyan
    & $jmeterPath -t $testPlanPath -JdataDir=$dataDir -Jhost=$Host -Jport=$Port
} else {
    # Run in Non-GUI mode (for actual testing)
    Write-Host "Starting load test in Non-GUI mode..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Test will run for $Duration seconds with 50 VUs" -ForegroundColor Cyan
    Write-Host "Press Ctrl+C to stop the test early" -ForegroundColor Gray
    Write-Host ""

    $jmeterArgs = @(
        "-n",
        "-t", $testPlanPath,
        "-l", $resultsFile,
        "-JdataDir=$dataDir",
        "-Jhost=$Host",
        "-Jport=$Port"
    )

    if ($GenerateReport) {
        $jmeterArgs += @("-e", "-o", $reportDir)
    }

    try {
        & $jmeterPath @jmeterArgs
        
        Write-Host ""
        Write-Host "==================================================" -ForegroundColor Cyan
        Write-Host "  Test Completed!" -ForegroundColor Green
        Write-Host "==================================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Results saved to: $resultsFile" -ForegroundColor Yellow
        
        if ($GenerateReport -and (Test-Path $reportDir)) {
            Write-Host "HTML Report: $reportDir\index.html" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "Opening report in browser..." -ForegroundColor Gray
            Start-Process "$reportDir\index.html"
        } else {
            Write-Host ""
            Write-Host "To generate HTML report, run:" -ForegroundColor Gray
            Write-Host "  .\run-tests.ps1 -GenerateReport" -ForegroundColor White
        }
    }
    catch {
        Write-Host "ERROR: Test execution failed!" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Usage Examples:" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "# Run in GUI mode (for debugging):" -ForegroundColor Gray
Write-Host "  .\run-tests.ps1 -GUI" -ForegroundColor White
Write-Host ""
Write-Host "# Run with HTML report generation:" -ForegroundColor Gray
Write-Host "  .\run-tests.ps1 -GenerateReport" -ForegroundColor White
Write-Host ""
Write-Host "# Run against different host:" -ForegroundColor Gray
Write-Host "  .\run-tests.ps1 -Host 192.168.1.100 -Port 8080" -ForegroundColor White
Write-Host ""
