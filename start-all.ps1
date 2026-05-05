# SmartDocs Full Stack Startup Script
# Starts both frontend and backend in separate windows

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "SmartDocs Full Stack Startup" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$scriptDir = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
$backendScript = Join-Path $scriptDir "start-backend.ps1"
$frontendScript = Join-Path $scriptDir "start-frontend.ps1"

# Check if scripts exist
if (-not (Test-Path $backendScript)) {
    Write-Host "❌ ERROR: start-backend.ps1 not found" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $frontendScript)) {
    Write-Host "❌ ERROR: start-frontend.ps1 not found" -ForegroundColor Red
    exit 1
}

Write-Host "Starting Backend..." -ForegroundColor Cyan
Start-Process PowerShell -ArgumentList "-NoExit -Command &'$backendScript'" -WindowStyle Normal

Start-Sleep -Seconds 3

Write-Host "Starting Frontend..." -ForegroundColor Cyan
Start-Process PowerShell -ArgumentList "-NoExit -Command &'$frontendScript'" -WindowStyle Normal

Write-Host ""
Write-Host "✓ Both services started in separate windows" -ForegroundColor Green
Write-Host ""
Write-Host "URLs:" -ForegroundColor Cyan
Write-Host "  Backend:  http://localhost:8080" -ForegroundColor Gray
Write-Host "  Frontend: http://localhost:3000" -ForegroundColor Gray
Write-Host ""
Write-Host "Close any window to stop that service." -ForegroundColor Gray
