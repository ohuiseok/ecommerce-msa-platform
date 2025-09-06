# 👤 User Service

사용자 관리 및 인증을 담당하는 마이크로서비스입니다.

## 🚀 빠른 시작

### 사전 요구사항
```bash
# PostgreSQL 실행 (Docker)
docker run -d --name user-db \
  -e POSTGRES_DB=ecommerce_user \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:15
```

### 개별 실행
```bash
# 1. 프로젝트 폴더로 이동
cd user-service

# 2. Eureka와 함께 실행
gradlew.bat bootRun

# 3. 독립 실행 (Eureka 없이)
gradlew.bat bootRun --args="--spring.profiles.active=standalone"
```

## 📋 주요 기능

- 🔐 사용자 회원가입/로그인
- 🎫 JWT 토큰 발급
- 👨‍💼 사용자 프로필 관리
- 🔒 Spring Security 기반 보안

## 🛠️ API 엔드포인트

```bash
# 회원가입
POST /users/register
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "phoneNumber": "010-1234-5678"
}

# 로그인
POST /users/login
{
  "email": "user@example.com",
  "password": "password123"
}

# 사용자 조회
GET /users/{userId}
Authorization: Bearer {JWT_TOKEN}

# 프로필 수정
PUT /users/{userId}
Authorization: Bearer {JWT_TOKEN}
```

## ⚙️ 설정

- **포트**: 8081
- **데이터베이스**: PostgreSQL (ecommerce_user)
- **프로필**: 
  - `default`: Eureka 연결
  - `standalone`: 독립 실행

## 🔧 환경변수

```bash
# 데이터베이스
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_user
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# Eureka (선택사항)
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://localhost:8761/eureka/
```

## 🧪 테스트

```bash
# 단위 테스트
gradlew.bat test

# 빌드
gradlew.bat build

# JAR 실행
java -jar build/libs/user-service-1.0.0.jar
```
