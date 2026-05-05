@echo off
REM SmartDocs Backend Startup Script (Batch Version)
REM This script automatically loads environment variables from .env file and starts the backend

echo ================================================
echo SmartDocs Backend Startup
echo ================================================
echo.

REM Get the directory of this script
set SCRIPT_DIR=%~dp0
set ENV_FILE=%SCRIPT_DIR%.env

REM Check if .env file exists
if not exist "%ENV_FILE%" (
    echo Error: .env file not found at %ENV_FILE%
    exit /b 1
)

echo Loading environment variables from .env file...

REM Load .env file variables
for /f "usebackq delims==" %%A in ("%ENV_FILE%") do (
    if not "%%A"=="" (
        if not "%%A:~0,1%"=="#" (
            set "%%A=%%B"
        )
    )
)

echo All environment variables loaded.
echo.

REM Check if OPENAI_API_KEY is set
if "%OPENAI_API_KEY%"=="" (
    echo Error: OPENAI_API_KEY not set in .env file
    exit /b 1
)

echo Starting backend...
if "%SERVER_PORT%"=="" set SERVER_PORT=8080

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":%SERVER_PORT% .*LISTENING"') do (
    echo Error: port %SERVER_PORT% is already in use by PID %%P
    echo Stop that process or change SERVER_PORT in .env before starting the backend.
    exit /b 1
)

echo Backend will be available at: http://localhost:%SERVER_PORT%
echo.

REM Start the backend
cd /d "%SCRIPT_DIR%"
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"

pause
