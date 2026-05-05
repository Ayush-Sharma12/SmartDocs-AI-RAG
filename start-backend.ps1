# SmartDocs Backend Startup Script
# This script automatically loads environment variables from .env file and starts the backend

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "SmartDocs Backend Startup" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Get the script directory
$scriptDir = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
$envFile = Join-Path $scriptDir ".env"
# Check if .env file exists
if (-not (Test-Path $envFile)) {
    Write-Host "Error: .env file not found at $envFile" -ForegroundColor Red
    exit 1
}

# Simple .env file loader
Write-Host "Loading environment variables from .env file..." -ForegroundColor Green

Get-Content $envFile | ForEach-Object {
    $_ = $_.Trim()
    
    # Skip empty lines and comments
    if ($_ -and -not $_.StartsWith("#")) {
        # Split on first = only
        $parts = $_ -split "=", 2
        if ($parts.Count -eq 2) {
            $key = $parts[0].Trim()
            $value = $parts[1].Trim()
            # Simple quote removal
            if ($value.StartsWith('"') -and $value.EndsWith('"')) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  Setting $key" -ForegroundColor Gray
        }
    }
}

Write-Host ""

# Verify OPENAI_API_KEY is set
if (-not $env:OPENAI_API_KEY) {
    Write-Host "Error: OPENAI_API_KEY not found in .env file" -ForegroundColor Red
    exit 1
}

Write-Host "OPENAI_API_KEY loaded successfully" -ForegroundColor Green
Write-Host ""

$serverPort = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { "8080" }
$listeningProcess = netstat -ano | Select-String ":$serverPort\s+.*LISTENING\s+(\d+)$" | Select-Object -First 1
if ($listeningProcess) {
    $conflictPid = $listeningProcess.Matches[0].Groups[1].Value
    Write-Host "Error: port $serverPort is already in use by PID $conflictPid" -ForegroundColor Red
    Write-Host "Stop that process or change SERVER_PORT in .env before starting the backend." -ForegroundColor Yellow
    exit 1
}

Write-Host "Starting SmartDocs Backend..." -ForegroundColor Cyan
Write-Host "Available at: http://localhost:$serverPort" -ForegroundColor Green
Write-Host ""

mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
