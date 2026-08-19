@echo off
setlocal

set APP_URL=http://localhost:8080/actuator/health

echo Checking ecommerce monolith...
curl -s -f %APP_URL%
if %errorlevel% neq 0 (
    echo Ecommerce monolith is unhealthy
    exit /b 1
)

echo Ecommerce monolith is healthy
pause
