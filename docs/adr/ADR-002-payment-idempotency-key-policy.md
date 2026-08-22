# ADR-002: 결제 idempotency key 정책

## 상태

Accepted

## 배경

현재 `PaymentService.requestPayment`는 결제 요청을 받을 때마다 `MockPgClient.charge`를 호출하고 결제 row를 새로 저장한다. 동일 주문에 대해 이미 `COMPLETED` 결제가 있으면 중복 결제를 거부하지만, 사용자가 새로고침하거나 앱이 타임아웃 후 재시도하는 경우 실패 결제 또는 처리 중 결제를 같은 요청으로 묶는 기준은 없다.

실제 PG 연동에서는 승인 요청이 성공했지만 서버 응답이 유실되거나, DB 저장 후 클라이언트가 실패로 인식해 같은 결제를 다시 보내는 상황이 발생할 수 있다. 결제 요청은 단순 중복 거부보다 "같은 요청이면 같은 결과 반환"을 우선해야 중복 과금과 클라이언트 재시도 혼선을 줄일 수 있다.

## 결정

결제 생성 API는 클라이언트가 생성한 `idempotencyKey`를 요청 본문에 포함한다. 서버는 `orderId + idempotencyKey` 조합을 결제 요청의 멱등 범위로 사용한다.

| 후보 | 결정 | 이유 |
| --- | --- | --- |
| 전역 `idempotencyKey` unique | 채택하지 않음 | 클라이언트가 UUID가 아닌 짧은 키를 쓰거나 여러 주문에서 같은 재시도 키를 재사용하면 불필요한 전역 충돌이 발생한다. |
| `userId + idempotencyKey` unique | 채택하지 않음 | 한 사용자가 여러 주문을 동시에 결제할 때 같은 키 생성 버그가 다른 주문 결제까지 막을 수 있다. |
| `orderId + idempotencyKey` unique | 채택 | 결제 재시도의 업무 의미가 특정 주문에 묶여 있고, 같은 주문의 같은 키만 같은 결제 시도로 판단하는 범위가 가장 좁다. |

## 요청 정책

- `idempotencyKey`는 클라이언트가 생성한 불투명 문자열로 다룬다.
- 권장 형식은 UUID 또는 동등한 충돌 저항성을 가진 랜덤 문자열이다.
- 서버는 키의 의미를 해석하지 않고, trim 후 빈 문자열은 거부한다.
- 키 길이는 1~100자로 제한한다.
- `orderId + idempotencyKey`가 이미 존재하면 새 PG 승인 요청을 보내지 않고 기존 결제 결과를 반환한다.
- 기존 결제 상태가 `COMPLETED`, `FAILED`, `CANCELLED`, `PENDING` 중 무엇이든 동일 키 재요청은 기존 row를 반환한다.
- 같은 주문에 다른 `idempotencyKey`로 다시 결제를 요청하는 것은 별도 결제 시도로 본다. 단, 이미 `COMPLETED` 결제가 있는 주문은 기존 정책대로 중복 결제를 거부한다.

## 저장 정책

- `payments` 테이블에 nullable `idempotency_key` 컬럼을 먼저 추가한다.
- 점진 배포 기간에는 기존 데이터와 내부 호출 호환성을 위해 nullable을 허용한다.
- API에서 키를 받기 시작한 뒤 `order_id, idempotency_key` unique index를 추가한다.
- PostgreSQL unique index는 nullable 컬럼의 여러 null을 허용하므로, 키가 없는 과거 row와 점진 배포 중 row는 유니크 제약 대상에서 사실상 제외된다.

## 보관 정책

결제 row 보관 기간과 idempotency key 보관 기간은 동일하게 둔다. 결제는 주문, 환불, 정산, CS 조회의 근거 데이터이므로 별도 TTL로 key만 삭제하지 않는다.

장기적으로 결제 데이터를 아카이브하는 경우에도 `orderId + idempotencyKey` 조합은 함께 이동해야 한다. 같은 주문에 대해 보관 기간 이후 동일 키 재요청을 새 요청으로 처리하는 정책은 현재 범위에서 허용하지 않는다.

## 충돌 처리

동시 요청으로 `orderId + idempotencyKey` unique 제약 충돌이 발생하면 서버는 실패 응답을 바로 반환하지 않는다. 트랜잭션을 정리한 뒤 같은 조합의 결제 row를 다시 조회해 기존 결과를 반환한다.

이 정책은 같은 요청의 재시도를 성공적으로 흡수하기 위한 것이며, 서로 다른 결제 수단이나 카드 번호를 같은 key로 보내는 클라이언트 오류는 서버가 별도 승인 없이 기존 결과를 반환한다. 클라이언트는 결제 시도마다 새 key를 만들고, 같은 결제 시도를 재시도할 때만 같은 key를 재사용해야 한다.

## 적용 범위

- `PaymentRequest.Create`에 `idempotencyKey` 필드를 추가한다.
- `Payment`에 `idempotencyKey` 컬럼을 추가한다.
- `PaymentRepository`에 `orderId + idempotencyKey` 조회 메서드를 추가한다.
- `PaymentService.requestPayment`는 PG 호출 전에 같은 key의 기존 결제를 조회하고, 있으면 바로 반환한다.
- unique 제약 충돌은 기존 결제 재조회로 처리한다.
- 결제 취소와 환불 idempotency는 별도 정책에서 다룬다.
- PG 웹훅 중복 처리는 ADR-006에서 별도 저장소와 판단 키를 정한다.

## 결과

결제 API의 재시도 의미는 특정 주문의 특정 결제 시도로 고정된다. 클라이언트가 같은 `idempotencyKey`를 재사용하면 서버는 PG를 다시 호출하지 않고 기존 결제 결과를 반환한다.

이 결정은 중복 과금 위험을 줄이지만, 클라이언트가 key 생성과 재사용 규칙을 지켜야 한다. 서버는 이후 구현 단계에서 nullable 컬럼, unique index, 기존 결과 반환 로직을 순서대로 추가해 배포 리스크를 낮춘다.
