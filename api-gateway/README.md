# 🌐 API Gateway

모든 마이크로서비스의 통합 진입점 역할을 하는 API 게이트웨이입니다.

## 🚀 빠른 시작

### 사전 요구사항
```bash
# Redis 실행 (JWT 토큰 관리용 - 선택사항)
docker run -d --name gateway-redis \
  -p 6379:6379 redis:7-alpine

# Eureka Server 실행 (서비스 디스커버리용)
cd ../eureka-server
gradlew.bat bootRun
```

### 개별 실행
```bash
cd api-gateway

# Eureka와 함께 실행 (권장)
gradlew.bat bootRun

# 독립 실행 (정적 라우팅)
gradlew.bat bootRun --args="--spring.profiles.active=standalone"
```

## 📋 주요 기능

- 🚪 **단일 진입점**: 모든 API 요청의 통합 관리
- 🔐 **JWT 인증**: 토큰 기반 인증/인가
- 🔄 **동적 라우팅**: Eureka 기반 서비스 디스커버리
- ⚡ **로드밸런싱**: 서비스 인스턴스 간 부하 분산
- 🛡️ **보안 필터**: CORS, Rate Limiting
- 📊 **모니터링**: 요청 추적 및 메트릭

## 🛠️ API 엔드포인트

### 사용자 서비스 라우팅
```bash
# 회원가입 (인증 불필요)
POST /api/users/register → user-service:8081/users/register

# 로그인 (인증 불필요)
POST /api/users/login → user-service:8081/users/login

# 사용자 정보 (JWT 필요)
GET /api/users/{id} → user-service:8081/users/{id}
```

### 상품 서비스 라우팅
```bash
# 상품 목록 (JWT 필요)
GET /api/products → product-service:8082/products

# 상품 검색 (JWT 필요)
GET /api/products/search?keyword=phone → product-service:8082/products/search?keyword=phone

# 상품 생성 (JWT 필요)
POST /api/products → product-service:8082/products
```

### 주문 서비스 라우팅
```bash
# 주문 생성 (JWT 필요)
POST /api/orders → order-service:8083/orders

# 주문 조회 (JWT 필요)
GET /api/orders/{id} → order-service:8083/orders/{id}
```

## ⚙️ 설정

- **포트**: 8080
- **인증**: JWT 기반
- **캐시**: Redis (선택사항)
- **서비스 디스커버리**: Eureka

## 🧪 테스트

### 라우팅 테스트
```bash
# 1. 사용자 등록 (인증 불필요)
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","name":"테스트"}'

# 2. 로그인하여 JWT 토큰 받기
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# 3. JWT 토큰으로 보호된 리소스 접근
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/products
```
