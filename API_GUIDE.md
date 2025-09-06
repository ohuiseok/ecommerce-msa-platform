# 🚀 API 사용 가이드

## 목차
- [서비스 시작하기](#서비스-시작하기)
- [API 테스트 예시](#api-테스트-예시)
- [서비스별 엔드포인트](#서비스별-엔드포인트)

## 서비스 시작하기

### 1. 전체 시스템 실행
```bash
# 전체 빌드 및 실행
./scripts/build-all.sh
docker-compose up --build

# 백그라운드 실행
docker-compose up -d --build
```

### 2. 개발환경 실행 (인프라만)
```bash
# 인프라만 실행
docker-compose -f docker-compose.dev.yml up -d

# 개별 서비스는 IDE에서 실행
```

### 3. 서비스 상태 확인
```bash
# 헬스체크
./scripts/health-check.sh

# 개별 서비스 확인
curl http://localhost:8761  # Eureka Dashboard
curl http://localhost:8080/actuator/health  # API Gateway
```

## API 테스트 예시

### 1. 사용자 관리 API

#### 회원가입
```bash
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "name": "홍길동",
    "phoneNumber": "010-1234-5678"
  }'
```

#### 로그인
```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

#### 사용자 정보 조회
```bash
# JWT 토큰이 필요합니다
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 2. 상품 관리 API

#### 상품 생성
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "스마트폰",
    "description": "최신 스마트폰입니다",
    "price": 899000,
    "stockQuantity": 100,
    "category": "전자제품",
    "brand": "TechBrand"
  }'
```

#### 상품 목록 조회
```bash
curl "http://localhost:8080/api/products?page=0&size=10&sort=createdAt,desc"
```

#### 상품 검색
```bash
curl "http://localhost:8080/api/products/search?keyword=스마트폰"
```

#### 카테고리별 상품 조회
```bash
curl "http://localhost:8080/api/products/category/전자제품"
```

#### 가격대별 상품 조회
```bash
curl "http://localhost:8080/api/products/price-range?minPrice=100000&maxPrice=1000000"
```

#### 재고 확인
```bash
curl http://localhost:8080/api/products/1/stock
```

### 3. 주문 관리 API

#### 주문 생성
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "userId": 1,
    "orderItems": [
      {
        "productId": 1,
        "quantity": 2
      }
    ],
    "shippingAddress": {
      "zipCode": "12345",
      "address": "서울시 강남구 테헤란로 123",
      "detailAddress": "456호",
      "recipientName": "홍길동",
      "recipientPhone": "010-1234-5678"
    }
  }'
```

#### 주문 조회
```bash
curl -X GET http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 사용자별 주문 목록
```bash
curl -X GET "http://localhost:8080/api/orders/user/1?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### 주문 상태 업데이트
```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "status": "CONFIRMED"
  }'
```

#### 주문 취소
```bash
curl -X DELETE http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 서비스별 엔드포인트

### Eureka Server
- **URL**: http://localhost:8761
- **Dashboard**: http://localhost:8761

### API Gateway
- **URL**: http://localhost:8080
- **Health**: http://localhost:8080/actuator/health

### User Service
- **Port**: 8081 (직접 접근 비권장)
- **Base Path**: `/api/users`

### Product Service  
- **Port**: 8082 (직접 접근 비권장)
- **Base Path**: `/api/products`

### Order Service
- **Port**: 8083 (직접 접근 비권장)
- **Base Path**: `/api/orders`

## 인증 흐름

1. **회원가입**: `POST /api/users/register`
2. **로그인**: `POST /api/users/login` → JWT 토큰 받기
3. **API 호출**: `Authorization: Bearer {token}` 헤더 포함

## 주요 특징

### Circuit Breaker
- User Service와 Product Service 간 통신 시 장애 격리
- 서비스 장애 시 Fallback 응답 제공

### Event-Driven Architecture
- 주문 생성/변경 시 Kafka 이벤트 발행
- 비동기 처리로 성능 향상

### 캐싱
- Product Service에서 Redis 캐싱 적용
- 상품 정보 조회 성능 최적화

### 분산 데이터베이스
- 각 서비스별 독립적인 데이터베이스 스키마
- 마이크로서비스 원칙 준수

## 모니터링

### Actuator 엔드포인트
```bash
# 각 서비스별 헬스체크
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Product Service
curl http://localhost:8083/actuator/health  # Order Service

# Circuit Breaker 상태 확인
curl http://localhost:8083/actuator/circuitbreakers
```

### 서비스 디스커버리
```bash
# 등록된 서비스 목록
curl http://localhost:8761/eureka/apps

# 특정 서비스 인스턴스 정보
curl http://localhost:8761/eureka/apps/USER-SERVICE
```

## 트러블슈팅

### 서비스가 시작되지 않는 경우
1. Docker 컨테이너 상태 확인: `docker-compose ps`
2. 로그 확인: `docker-compose logs [service-name]`
3. 포트 충돌 확인: `netstat -an | grep LISTEN`

### 서비스 간 통신 실패
1. Eureka에서 서비스 등록 상태 확인
2. Circuit Breaker 상태 확인
3. 네트워크 연결 상태 확인

### 데이터베이스 연결 실패
1. PostgreSQL 컨테이너 상태 확인
2. 데이터베이스 생성 여부 확인
3. 연결 정보 (URL, 사용자명, 비밀번호) 확인
