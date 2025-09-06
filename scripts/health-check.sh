#!/bin/bash

# MSA E-Commerce Platform 헬스체크 스크립트

echo "🏥 Checking health of all services..."

# 서비스 URL 정의
EUREKA_URL="http://localhost:8761/actuator/health"
GATEWAY_URL="http://localhost:8080/actuator/health"
USER_URL="http://localhost:8081/actuator/health"
PRODUCT_URL="http://localhost:8082/actuator/health"
ORDER_URL="http://localhost:8083/actuator/health"

# 헬스체크 함수
check_health() {
    local service_name=$1
    local url=$2
    
    echo -n "Checking $service_name... "
    
    if curl -s --connect-timeout 5 "$url" > /dev/null; then
        echo "✅ Healthy"
        return 0
    else
        echo "❌ Unhealthy"
        return 1
    fi
}

# 모든 서비스 헬스체크
check_health "Eureka Server" "$EUREKA_URL"
check_health "API Gateway" "$GATEWAY_URL"
check_health "User Service" "$USER_URL"
check_health "Product Service" "$PRODUCT_URL"
check_health "Order Service" "$ORDER_URL"

echo ""
echo "🔍 Checking Eureka service registrations..."
curl -s "http://localhost:8761/eureka/apps" | grep -o '<name>[^<]*</name>' | sed 's/<name>\|<\/name>//g' | sort | uniq

echo ""
echo "✅ Health check completed!"
