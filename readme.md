# E-Commerce Monolith

Spring Boot 기반 단일 이커머스 애플리케이션입니다. 회원, 상품, 주문 도메인을 하나의 배포 단위에서 제공합니다.

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

## 기술 스택

- Java 17
- Spring Boot 3.2
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT
- Docker Compose

## 실행 방법

### Docker Compose

```bash
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=admin123 docker-compose up --build
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
| `ADMIN_EMAIL` | 없음 | 시작 시 생성/갱신할 관리자 이메일 |
| `ADMIN_PASSWORD` | 없음 | 시작 시 생성/갱신할 관리자 비밀번호 |
| `ADMIN_NAME` | `Administrator` | 관리자 이름 |

운영 환경에서는 `JWT_SECRET`, DB 계정, `SPRING_JPA_HIBERNATE_DDL_AUTO`를 반드시 별도로 설정해야 합니다.
관리자 계정은 `ADMIN_EMAIL`과 `ADMIN_PASSWORD`가 모두 설정된 경우에만 생성되거나 갱신됩니다.

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

주문 API는 JWT 인증이 필요합니다. 사용자별 조회, 단건 조회, 취소는 본인 주문 또는 관리자만 접근할 수 있습니다. 상태별 주문 조회와 주문 상태 변경은 관리자만 접근할 수 있습니다.

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

## 남은 개선 과제

- 재고 차감은 조건부 update 쿼리로 처리하며, 동시 주문 시나리오 테스트를 추가해야 합니다.
- 운영용 DB 마이그레이션 도입: Flyway 또는 Liquibase
- 통합 테스트 추가: 회원가입/로그인, 재고 차감, 주문 생성/취소, 관리자 권한

## 빌드

```bash
./gradlew clean bootJar
```

헬스체크:

```bash
./scripts/health-check.sh
```
