@echo off
echo 🏗️ Building MSA E-Commerce Platform...

cd /d "%~dp0\.."

echo 📦 Building all services...

echo Building Eureka Server...
cd eureka-server
call gradlew.bat clean bootJar
if %errorlevel% neq 0 (
    echo ❌ Failed to build Eureka Server
    exit /b 1
)
cd ..

echo Building API Gateway...
cd api-gateway
call gradlew.bat clean bootJar
if %errorlevel% neq 0 (
    echo ❌ Failed to build API Gateway
    exit /b 1
)
cd ..

echo Building User Service...
cd user-service
call gradlew.bat clean bootJar
if %errorlevel% neq 0 (
    echo ❌ Failed to build User Service
    exit /b 1
)
cd ..

echo Building Product Service...
cd product-service
call gradlew.bat clean bootJar
if %errorlevel% neq 0 (
    echo ❌ Failed to build Product Service
    exit /b 1
)
cd ..

echo Building Order Service...
cd order-service
call gradlew.bat clean bootJar
if %errorlevel% neq 0 (
    echo ❌ Failed to build Order Service
    exit /b 1
)
cd ..

echo ✅ All services built successfully!
echo 🚀 You can now run: docker-compose up --build
pause
