# E-Commerce Monolith

Spring Boot 기반 이커머스 애플리케이션입니다. 기존 MSA 구조의 `user-service`, `product-service`, `order-service` 기능을 하나의 모놀리식 애플리케이션으로 통합했습니다.

## 현재 구조

```text
src/main/java/com/ecommerce/monolith/
├── EcommerceApplication.java
├── common/      # JPA auditing, 공통 예외 응답
├── security/    # JWT 인증 필터
├── user/        # 회원가입, 로그인, 사용자 관리
├── product/     # 상품 조회, 검색, 재고 관리
└── order/       # 주문 생성, 조회, 상태 변경, 취소
```

기존 MSA 디렉터리(`api-gateway`, `eureka-server`, `user-service`, `product-service`, `order-service`)는 전환 참고용으로 남아 있습니다. 실제 실행 기준은 루트 Spring Boot 앱입니다.

## 기술 스택

- Java 17
- Spring Boot 3.2
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT
- Docker Compose

MSA 전용 구성인 Eureka, API Gateway, OpenFeign, Resilience4j, Kafka, Zookeeper는 모놀리식 실행 경로에서 제거했습니다.

## 실행 방법

### Docker Compose

```bash
docker-compose up --build
```

애플리케이션:

- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- PostgreSQL: localhost:5432

### 개발용 DB만 실행

```bash
docker-compose -f docker-compose.dev.yml up -d
```

그 다음 로컬에서 애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

Gradle wrapper가 포함되어 있어 로컬 Gradle 설치 없이 실행할 수 있습니다.

## 환경 변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 애플리케이션 포트 |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/ecommerce` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB 사용자 |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB 비밀번호 |
| `JWT_SECRET` | 개발용 기본값 | JWT 서명 키 |
| `JWT_EXPIRATION` | `86400000` | Access token 만료 시간(ms) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Hibernate DDL 전략 |

운영 환경에서는 `JWT_SECRET`, DB 계정, `SPRING_JPA_HIBERNATE_DDL_AUTO`를 반드시 별도로 설정해야 합니다.

## API 경로

모든 API는 단일 앱의 `/api` 하위로 제공됩니다.

### User

- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/users/{userId}`
- `GET /api/users/email/{email}`
- `PUT /api/users/{userId}`

### Product

- `POST /api/products`
- `GET /api/products`
- `GET /api/products/{productId}`
- `GET /api/products/search?keyword=...`
- `GET /api/products/category/{category}`
- `GET /api/products/brand/{brand}`
- `GET /api/products/categories`
- `GET /api/products/brands`
- `GET /api/products/price-range?minPrice=...&maxPrice=...`
- `GET /api/products/{productId}/stock`
- `PUT /api/products/{productId}`
- `PUT /api/products/{productId}/stock`
- `DELETE /api/products/{productId}`

상품 조회 `GET /api/products/**`는 공개 API이고, 생성/수정/삭제/재고 변경은 `ADMIN` 권한 JWT가 필요합니다.

### Order

- `POST /api/orders`
- `GET /api/orders/{orderId}`
- `GET /api/orders/user/{userId}`
- `GET /api/orders/status/{status}`
- `PUT /api/orders/{orderId}/status`
- `DELETE /api/orders/{orderId}`

주문 API는 JWT 인증이 필요합니다.

## 인증 흐름

회원가입:

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

로그인:

```bash
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

인증 API 호출:

```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 모놀리식 전환 내용

- 서비스별 Spring Boot 앱을 루트 단일 앱으로 통합했습니다.
- Gateway 라우팅 대신 컨트롤러가 `/api/...` 경로를 직접 제공합니다.
- 주문 서비스의 Feign 호출을 `UserService`, `ProductService` 직접 호출로 교체했습니다.
- 주문 생성과 재고 차감은 같은 애플리케이션 트랜잭션 흐름에서 처리됩니다.
- Redis/Kafka/Eureka/Zookeeper 컨테이너를 기본 Compose에서 제거했습니다.
- PostgreSQL은 단일 `ecommerce` 데이터베이스를 사용합니다.

## 남은 개선 과제

- 재고 차감은 조건부 update 쿼리로 처리하며, 동시 주문 시나리오 테스트를 추가해야 합니다.
- 운영용 DB 마이그레이션 도입: Flyway 또는 Liquibase
- 사용자별 조회/수정 접근 제한 강화
- 통합 테스트 추가: 회원가입/로그인, 재고 차감, 주문 생성/취소, 관리자 권한

## 빌드

```bash
./gradlew clean bootJar
```

헬스체크:

```bash
./scripts/health-check.sh
```
