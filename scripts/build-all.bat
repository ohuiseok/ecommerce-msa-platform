@echo off
setlocal

cd /d "%~dp0\.."

echo Building ecommerce monolith...
call gradlew.bat clean bootJar
if %errorlevel% neq 0 (
    echo Failed to build ecommerce monolith
    exit /b 1
)

echo Build complete. Run: docker-compose up --build
pause
