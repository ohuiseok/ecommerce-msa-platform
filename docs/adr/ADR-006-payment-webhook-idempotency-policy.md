# ADR-006: PG 웹훅 중복 판단 키와 이벤트 보관 기간

## 상태

Accepted

## 배경

Phase 2 Day 6에서 Mock PG 이벤트 모델은 `eventId`, `deliveryId`, `type`, `orderId`, `transactionId`, `amount`, `failureReason`, `occurredAt`을 가진다. 같은 결제 결과가 네트워크 오류나 PG 재시도 정책으로 여러 번 전달될 수 있도록 `duplicateDelivery`는 같은 `eventId`와 새 `deliveryId`를 가진 이벤트를 만든다.

현재 결제 API의 idempotency는 ADR-002에 따라 `orderId + idempotencyKey`로 결제 생성 요청을 보호한다. 그러나 PG 웹훅은 클라이언트 요청이 아니라 외부 결제 시스템의 사후 이벤트이므로 별도 저장소와 중복 판단 기준이 필요하다.

웹훅 중복 처리가 없으면 같은 승인, 실패, 취소 이벤트가 반복 전달될 때 주문 상태 변경, 운영 보정 등록, 향후 환불 요청 같은 후속 처리가 중복 실행될 수 있다. 특히 ADR-008에 따라 만료 취소된 주문에 늦은 결제 승인 이벤트가 도착하면 운영 보정 대상으로 등록해야 하는데, 같은 이벤트 재전송마다 보정 작업이 늘어나면 운영자가 실제 건수를 잘못 판단하게 된다.

## 결정

PG 웹훅 처리 idempotency는 PG가 부여한 업무 이벤트 식별자인 `eventId`를 기준으로 판단한다. `deliveryId`는 같은 이벤트의 전달 시도를 구분하는 값으로만 저장하고, 중복 처리 방지 키로 사용하지 않는다.

| 후보 | 결정 | 이유 |
| --- | --- | --- |
| `eventId` unique | 채택 | 같은 결제 결과 이벤트가 여러 번 전달되어도 업무 이벤트는 하나이므로 후속 상태 변경도 한 번만 실행해야 한다. Mock PG의 중복 전달 모델과도 일치한다. |
| `deliveryId` unique | 채택하지 않음 | 재전송마다 값이 달라지는 전달 시도 ID이므로 중복 웹훅을 막을 수 없다. 전달 이력 관찰용으로만 적합하다. |
| `transactionId + type` unique | 채택하지 않음 | 실패 이벤트처럼 거래 ID가 없을 수 있고, PG별 이벤트 생성 규칙을 서버가 추론하게 된다. |
| `orderId + type + amount` unique | 채택하지 않음 | 같은 주문에 대해 실패 후 재시도, 취소, 부분 환불 같은 이벤트가 늘어나면 정상 이벤트까지 충돌할 수 있다. |

## 저장소 정책

`payment_webhook_events` 테이블을 추가해 수신한 웹훅의 처리 상태를 저장한다.

| 컬럼 | 정책 |
| --- | --- |
| `webhook_event_id` | 내부 PK |
| `pg_event_id` | PG 업무 이벤트 ID, not null, unique |
| `pg_delivery_id` | PG 전달 시도 ID, not null |
| `event_type` | PG 이벤트 타입, not null |
| `order_id` | 이벤트 대상 주문 ID, nullable 허용 |
| `pg_transaction_id` | 승인 또는 취소 거래 ID, nullable 허용 |
| `amount` | PG 이벤트 금액, nullable 허용 |
| `status` | `RECEIVED`, `PROCESSED`, `FAILED`, `IGNORED` |
| `failure_reason` | 처리 실패 사유, nullable |
| `pg_occurred_at` | PG 이벤트 발생 시각, not null |
| `received_at` | 서버 수신 시각, not null |
| `processed_at` | 처리 완료 또는 무시 시각, nullable |
| `created_at`, `updated_at` | 감사 시각 |

`pg_event_id` unique 제약이 중복 처리의 최종 방어선이다. 애플리케이션은 웹훅을 받으면 먼저 `pg_event_id`로 기존 row를 조회하고, 이미 존재하면 후속 도메인 처리를 실행하지 않고 기존 처리 결과를 반환한다. 동시 수신으로 unique 충돌이 발생하면 트랜잭션을 정리한 뒤 같은 `pg_event_id` row를 재조회한다.

## 처리 상태 정책

- `RECEIVED`: 웹훅을 저장했지만 후속 도메인 처리가 끝나지 않은 상태다.
- `PROCESSED`: 주문, 결제, 운영 보정 등록 등 필요한 후속 처리가 정상 완료된 상태다.
- `FAILED`: 일시적 장애 또는 검증 실패로 처리를 완료하지 못한 상태다. 재처리 대상이 될 수 있다.
- `IGNORED`: 중복이거나 현재 정책상 후속 처리가 필요 없는 이벤트다.

중복 수신된 이벤트는 새 row를 만들지 않는다. 기존 row의 `pg_delivery_id`를 덮어쓰지 않고, 필요한 경우 로그에 새 전달 ID만 남긴다. 전달 시도별 상세 이력이 필요해지면 별도 `payment_webhook_deliveries` 테이블을 추가하되, 현재 단계에서는 이벤트 단위 저장소만 둔다.

## 보관 정책

웹훅 idempotency row는 결제 row와 같은 보관 기간을 따른다. 웹훅은 결제 상태 변경, 주문 보정, 환불 판단의 감사 근거이므로 단기 TTL로 삭제하지 않는다.

장기 보관 비용이 문제가 되면 결제 데이터 아카이브와 함께 `pg_event_id`, 이벤트 타입, 주문 ID, 거래 ID, 처리 상태, 주요 시각을 보존한다. 운영 DB에서 아카이브된 이후 같은 `pg_event_id`가 다시 도착하는 경우는 비정상적인 장기 지연 이벤트로 보고 자동 상태 변경하지 않고 운영 보정 대상으로 남긴다.

## 적용 범위

- 웹훅 수신 저장소는 결제 생성 API의 `idempotencyKey`와 별개로 관리한다.
- 중복 판단의 기준은 `pg_event_id` unique 제약이다.
- `pg_delivery_id`는 로그와 장애 분석용 필드로 저장한다.
- Phase 2 Day 9의 웹훅 상태 반영 로직은 새 이벤트 저장 성공 후에만 주문과 결제 후속 처리를 실행한다.
- 늦은 결제 승인 이벤트의 운영 보정 등록은 `pg_event_id` 기준으로 한 번만 실행한다.
- PG 서명 검증, 재시도 backoff, dead-letter 전환은 실제 외부 PG 연동 범위에서 별도 정책으로 확장한다.

## 결과

PG 웹훅은 클라이언트 결제 요청 idempotency와 분리된 이벤트 단위 idempotency 저장소로 보호된다. 같은 `eventId`가 여러 `deliveryId`로 재전송되어도 후속 도메인 처리는 한 번만 실행한다.

이 결정은 중복 상태 변경과 중복 보정 작업을 줄인다. 다음 구현 단계는 `payment_webhook_events` 저장소를 추가하고, 새 이벤트로 저장된 경우에만 웹훅 상태 반영 로직을 실행하는 흐름에 집중한다.
