@echo off
echo 🏥 Checking health of all services...

set EUREKA_URL=http://localhost:8761/actuator/health
set GATEWAY_URL=http://localhost:8080/actuator/health
set USER_URL=http://localhost:8081/actuator/health
set PRODUCT_URL=http://localhost:8082/actuator/health
set ORDER_URL=http://localhost:8083/actuator/health

echo Checking Eureka Server...
curl -s -f %EUREKA_URL% >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Eureka Server - Healthy
) else (
    echo ❌ Eureka Server - Unhealthy
)

echo Checking API Gateway...
curl -s -f %GATEWAY_URL% >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ API Gateway - Healthy
) else (
    echo ❌ API Gateway - Unhealthy
)

echo Checking User Service...
curl -s -f %USER_URL% >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ User Service - Healthy
) else (
    echo ❌ User Service - Unhealthy
)

echo Checking Product Service...
curl -s -f %PRODUCT_URL% >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Product Service - Healthy
) else (
    echo ❌ Product Service - Unhealthy
)

echo Checking Order Service...
curl -s -f %ORDER_URL% >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ Order Service - Healthy
) else (
    echo ❌ Order Service - Unhealthy
)

echo.
echo 🔍 Checking Eureka service registrations...
curl -s "http://localhost:8761/eureka/apps" 2>nul | findstr "<name>"

echo.
echo ✅ Health check completed!
pause
