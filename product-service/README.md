# 📦 Product Service

상품 관리, 검색, 재고 관리를 담당하는 마이크로서비스입니다.

## 🚀 빠른 시작

### 사전 요구사항
```bash
# PostgreSQL 실행
docker run -d --name product-db \
  -e POSTGRES_DB=ecommerce_product \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:15

# Redis 실행 (캐싱용 - 선택사항)
docker run -d --name product-redis \
  -p 6379:6379 redis:7-alpine
```

### 개별 실행
```bash
cd product-service

# Eureka와 함께 실행
gradlew.bat bootRun

# 독립 실행 (Eureka 없이)
gradlew.bat bootRun --args="--spring.profiles.active=standalone"
```

## 📋 주요 기능

- 📦 상품 CRUD 관리
- 🔍 상품 검색 (PostgreSQL Full-Text Search)
- 📊 카테고리별 상품 관리
- 📈 재고 관리 및 추적
- ⚡ Redis 캐싱으로 성능 최적화

## 🛠️ API 엔드포인트

```bash
# 상품 생성
POST /products
{
  "name": "스마트폰",
  "description": "최신 스마트폰",
  "price": 899000,
  "stockQuantity": 100,
  "category": "전자제품",
  "brand": "TechBrand"
}

# 상품 목록 조회
GET /products?page=0&size=10&sort=createdAt,desc

# 상품 검색
GET /products/search?keyword=스마트폰

# 카테고리별 조회
GET /products/category/전자제품

# 가격대별 조회
GET /products/price-range?minPrice=100000&maxPrice=1000000

# 재고 확인
GET /products/{id}/stock

# 재고 업데이트
PUT /products/{id}/stock
{
  "quantity": 10,
  "operation": "DECREASE"
}
```

## ⚙️ 설정

- **포트**: 8082
- **데이터베이스**: PostgreSQL (ecommerce_product)
- **캐시**: Redis (선택사항)
- **프로필**: 
  - `default`: 전체 기능 (Eureka + Redis)
  - `standalone`: 독립 실행

## 🔧 환경변수

```bash
# 데이터베이스
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_product
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Redis (선택사항)
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# 캐시 TTL
CACHE_TTL_PRODUCT_INFO=600000  # 10분
CACHE_TTL_PRODUCT_LIST=180000  # 3분
```

## 🏗️ 데이터베이스 스키마

```sql
CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    category VARCHAR(100),
    brand VARCHAR(100),
    image_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 검색 성능을 위한 인덱스
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_name_search ON products USING GIN(to_tsvector('korean', name));
CREATE INDEX idx_products_price ON products(price);
```

## 🧪 테스트

```bash
# 단위 테스트
gradlew.bat test

# 빌드
gradlew.bat build

# 성능 테스트 (옵션)
# Apache Bench 설치 후
ab -n 100 -c 10 http://localhost:8082/products
```
