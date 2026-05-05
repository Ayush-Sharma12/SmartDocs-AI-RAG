# SmartDocs Frontend Startup Script

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "SmartDocs Frontend Startup" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Get the script directory
$scriptDir = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
$frontendDir = Join-Path $scriptDir "frontend"

# Check if frontend directory exists
if (-not (Test-Path $frontendDir)) {
    Write-Host "❌ ERROR: frontend directory not found" -ForegroundColor Red
    exit 1
}

Write-Host "📂 Navigating to frontend directory..." -ForegroundColor Green
Set-Location $frontendDir

# Check if node_modules exists
if (-not (Test-Path "node_modules")) {
    Write-Host "📦 Installing dependencies (npm install)..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ npm install failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Dependencies installed" -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting frontend..." -ForegroundColor Cyan
Write-Host "Frontend will be available at: http://localhost:3000" -ForegroundColor Green
Write-Host ""

# Start the frontend
npm run dev
