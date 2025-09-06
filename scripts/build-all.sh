#!/bin/bash

# MSA E-Commerce Platform 전체 빌드 스크립트

echo "🏗️  Building MSA E-Commerce Platform..."

# 루트 디렉토리로 이동
cd "$(dirname "$0")/.."

# 모든 서비스 빌드
echo "📦 Building all services..."

echo "Building Eureka Server..."
cd eureka-server
./gradlew clean bootJar
cd ..

echo "Building API Gateway..."
cd api-gateway
./gradlew clean bootJar
cd ..

echo "Building User Service..."
cd user-service
./gradlew clean bootJar
cd ..

echo "Building Product Service..."
cd product-service
./gradlew clean bootJar
cd ..

echo "Building Order Service..."
cd order-service
./gradlew clean bootJar
cd ..

echo "✅ All services built successfully!"
echo "🚀 You can now run: docker-compose up --build"
