# API Guide

이 문서는 모놀리식으로 통합된 이커머스 애플리케이션 기준입니다.

## 서비스 시작

```bash
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=admin123 docker-compose up --build
```

개발 중 DB만 띄울 때:

```bash
docker-compose -f docker-compose.dev.yml up -d
./gradlew bootRun
```

상태 확인:

```bash
curl http://localhost:8080/actuator/health
```

## 인증

관리자 권한이 필요한 API는 `ADMIN_EMAIL`과 `ADMIN_PASSWORD`로 부트스트랩한 관리자 계정으로 로그인한 뒤 받은 토큰을 사용합니다.

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

로그인 응답의 `accessToken`을 인증 API에 전달합니다.

```bash
Authorization: Bearer YOUR_JWT_TOKEN
```

## User API

사용자 조회:

```bash
curl http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

사용자 수정:

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "name": "홍길동",
    "phoneNumber": "010-1111-2222"
  }'
```

## Product API

상품 생성은 관리자 토큰이 필요합니다.

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "name": "스마트폰",
    "description": "최신 스마트폰입니다",
    "price": 899000,
    "stockQuantity": 100,
    "category": "전자제품",
    "brand": "TechBrand"
  }'
```

상품 목록:

```bash
curl "http://localhost:8080/api/products?page=0&size=10&sort=createdAt,desc"
```

상품 검색:

```bash
curl "http://localhost:8080/api/products/search?keyword=스마트폰"
```

재고 확인:

```bash
curl http://localhost:8080/api/products/1/stock
```

재고 변경은 관리자 토큰이 필요합니다.

```bash
curl -X PUT http://localhost:8080/api/products/1/stock \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "quantity": 5,
    "operation": "DECREASE"
  }'
```

## Cart API

장바구니 담기:

```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

장바구니 조회:

```bash
curl http://localhost:8080/api/cart \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

장바구니 항목 수량 변경:

```bash
curl -X PUT http://localhost:8080/api/cart/items/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"quantity": 3}'
```

## Order API

주문 생성 (상품을 직접 지정):

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
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

장바구니 기반 체크아웃 (담긴 상품으로 주문을 만들고 장바구니를 비웁니다):

```bash
curl -X POST http://localhost:8080/api/orders/checkout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "shippingAddress": {
      "zipCode": "12345",
      "address": "서울시 강남구 테헤란로 123",
      "detailAddress": "456호",
      "recipientName": "홍길동",
      "recipientPhone": "010-1234-5678"
    }
  }'
```

주문 조회:

```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

사용자별 주문 목록:

```bash
curl "http://localhost:8080/api/orders/user/1?page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

주문 상태 변경은 관리자 토큰이 필요합니다.

```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "status": "CONFIRMED"
  }'
```

주문 취소:

```bash
curl -X DELETE http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Payment API

결제 요청 (모의 PG. `CARD` 결제는 카드번호 마지막 자리가 짝수면 승인, 홀수면 거절됩니다):

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "orderId": 1,
    "method": "CARD",
    "cardNumber": "4111111111111112"
  }'
```

결제가 승인되면 해당 주문 상태가 자동으로 `CONFIRMED`로 바뀝니다. 응답의 `status` 필드가 `COMPLETED`/`FAILED`인지로 결과를 확인합니다.

주문별 결제 조회:

```bash
curl http://localhost:8080/api/payments/order/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

결제 취소/환불 (관리자 전용):

```bash
curl -X DELETE http://localhost:8080/api/payments/1 \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

## API 문서 (Swagger UI)

```bash
open http://localhost:8080/swagger-ui.html
```

우측 상단 `Authorize` 버튼에 `Bearer YOUR_JWT_TOKEN`을 입력하면 인증이 필요한 API도 UI에서 바로 호출할 수 있습니다.
