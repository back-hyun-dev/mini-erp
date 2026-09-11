# 재고 관리 미니 ERP System

Spring Boot와 JPA 기반으로 구축한 재고 관리 및 동시성 제어 엔진입니다.  
고객 요청이 몰릴 때 발생하는 **동시성 이슈(Race Condition)** 와 **초과판매(Overselling)** 문제를 방지하기 위해, 도메인 불변식 수립 및 우선적으로 JPA 낙관적 락(Optimistic Lock) 기반의 방어 체계를 구축했습니다.

---

## Dev Logs (개발기)
- [[Java/Spring] mini-ERP 개발기 #1 - Stock 엔티티 설계](https://velog.io/@back-hyun-dev/JavaSpring-mini-ERP-%EA%B0%9C%EB%B0%9C%EA%B8%B0-1-Stock-%EC%97%94%ED%8B%B0%ED%8B%B0-%EC%84%A4%EA%B3%84%EC%99%80-%EB%8B%A8%EC%9C%84-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EA%B5%AC%EC%B6%95)
- [[Java/Spring] mini-ERP 개발기 #2 - StockHistory 엔티티 설계](https://velog.io/@back-hyun-dev/JavaSpring-mini-ERP-%EA%B0%9C%EB%B0%9C%EA%B8%B0-2-StockHistory-%EC%97%94%ED%8B%B0%ED%8B%B0-%EC%84%A4%EA%B3%84)
- [[Java/Spring] mini-ERP 개발기 #3 - StockService 구현](https://velog.io/@back-hyun-dev/JavaSpring-mini-ERP-%EA%B0%9C%EB%B0%9C%EA%B8%B0-3-StockService-%EA%B5%AC%ED%98%84)
---


## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 4.1.1
- **ORM / Database**: Spring Data JPA, mySQL
- **Build Tool**: Gradle
- **Testing**: JUnit 5, AssertJ

---

## 주요 기능 및 핵심 설계
- **3단계 재고 라이프사이클 관리**: 결제 시점 선점(`reserve`), 물류 출고 차감(`decrease`), 재물조사 수량 조정(`adjust`)
- **도메인 정합성 보장**: JPA `Optional` 기반 안전한 예외 처리 및 도메인 엔티티 내 불변식 검증
- **동시성 제어 및 Race Condition 방지**: 멀티스레드 환경에서의 재고 차감 정합성 보장
- **글로벌 예외 파이프라인 구축**: 예외 응답을 ErrorResponse DTO 및 ErrorCode Enum 기반 정적 규격으로 하여 글로벌 파이프라인 표준화

---

## Key Engineering Decisions & Architecture

### 1. 동시성 제어 및 도메인 정합성을 위한 2단계 방어 체계 구축
* **3단계 재고 라이프사이클 분리**: 초과 판매(Overselling) 차단을 위해 물리 재고(`quantity`)와 선점 재고(`allocatedQuantity`)를 분리. 결제 시점엔 가용 재고 범위 내 선점(reserve), 물류 출고 시점엔 실제 차감(decrease) 진행.
* **도메인 불변식(Invariant) 보장**: 엔티티 내에 `quantity >= allocatedQuantity >= 0` 규칙을 최우선으로 수립. 실무 재물조사 조정(adjust) 시 선점 수량보다 적은 수치로 변경 시 예외를 발생시켜 가용 재고 음수 상태를 완벽히 차단.
* **수학적 조건 최적화**: `reserve()` 시점에 가용 재고 검증이 완료되므로, 출고(`decrease`) 시에는 선점 수량 검증만 수행. 중복 검사를 제거하여 도메인 메서드의 책임을 단축하고 비즈니스 의도를 명확히 표현.
* **Persistence Layer 동시성 제어**: JPA `@Version` 기반 낙관적 락(Optimistic Locking)을 적용하여, DB 병목을 최소화하면서 동시 수정 충돌 시 데이터 정합성 보장.

### 2. 대규모 분산 환경을 고려한 식별자(PK) 확장 전략 (YAGNI 원칙)
* **Zero-Cost Abstraction**: 초기 개발 단계에서는 오버 엔지니어링을 지양하고 가벼운 Long (Auto-Increment) 방식을 채택하여 빠른 개발 속도 및 DB 인덱스 성능 확보.
* **Future-Proof Strategy**: PK 규격을 64비트 정수(Long)로 고정함으로써, 향후 분산 DB(샤딩) 환경으로 확장하더라도 DB 스키마 변경 없이 애플리케이션 채번 알고리즘(TSID/Snowflake) 교체만으로 전환 가능하도록 설계.

### 3. 데이터 불변성(Immutability) 및 간접 참조 기반의 추적성(Audit Trail) 확보
* **간접 참조(Decoupling) 적용**: `@ManyToOne` 엔티티 직접 참조 대신 `stockId`(`Long`) 간접 참조를 채택하여 도메인 간 결합도를 제거하고, 연관 관계 탐색으로 인한 N+1 및 GC 오버헤드 차단.
* **불변 이력 엔티티(`updatable = false`)**: 모든 이력 컬럼에 수정 불가 제약을 부여하여 과거 재고 변동 기록의 위변조 가능성을 원천 차단.
* **정적 팩토리 메서드를 통한 도메인 안전성 확보**: `private` 생성자 기반으로 `createAutoHistory`(주문 연관 자동 적재)와 `createManualHistory`(관리자 수동 조정)를 분리하여, 인자 순서 오류나 필드 누락으로 인한 데이터 결함을 컴파일 및 객체 생성 시점에 방지.
* **재고 상태 복원 및 감사(Audit)를 위한 스냅샷 저장**: 단순 변동량(`amount`) 외에도 변동 직후의 물리 재고(`snapshotQuantity`) 및 선점 재고(`snapshotAllocatedQuantity`) 스냅샷을 함께 보존합니다. 이를 통해 과거 전체 이력을 집계하는 $O(N)$ 연산 없이 **특정 시점의 재고 상태를 $O(1)$ 연산으로 조회 및 복원**할 수 있습니다.

### 4. 레이어별 검증 역할 분리와 글로벌 예외 파이프라인 표준화
* **글로벌 예외 핸들링 (`@RestControllerAdvice`)**: 일관성 있는 예외 처리를 위해 `ErrorResponse` DTO와 `ErrorCode` Enum 기반의 정적 타입 응답 구조로 표준 예외 파이프라인 구현.
* **계층별 Fast-Fail 및 책임 분리**:
    * **Controller / DTO**: 요청 진입 시점에서 `@Valid` 어노테이션으로 바인딩 오류 및 유효하지 않은 입력값을 1차 차단하여 불필요한 DB I/O 비용 차단.
    * **Service Layer**: `@Transactional(readOnly = true)` 기반 조회 최적화와 함께, 미등록 재고 접근 시 `BusinessException(STOCK_NOT_FOUND)`을 던져 후속 비즈니스 로직 진행 차단.
---

## Domain Model Specification

### 1. Stock (재고 엔티티)
> 물리 재고와 선점 재고를 분리하여 동시성 및 가용 재고 불변식을 관리합니다.

| Field | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | Long | PK (Auto-Increment) | 재고 식별자 |
| `productId` | Long | Not Null, Unique | 상품 ID (간접 참조) |
| `quantity` | Integer | Not Null | 물리적 재고 수량 |
| `allocatedQuantity` | Integer | Not Null | 선점(결제 대기) 재고 수량 |
| `version` | Long | @Version | 낙관적 락(Optimistic Lock) 버전 |
| `createdAt` | LocalDateTime | Not Null, Unupdatable | 최초 등록 일시 |
| `updatedAt` | LocalDateTime | Not Null | 최종 수량 변경 일시 |

### 2. StockHistory (재고 변동 이력 엔티티)
> 데이터 불변성(Immutability)과 정합성을 최우선으로 고려한 이력 트래킹 엔티티입니다.

| Field | Type | Constraint | Description |
| :--- | :--- | :--- | :--- |
| `id` | Long | PK (Auto-Increment) | 이력 식별자 |
| `stockId` | Long | Not Null, Unupdatable | 대상 재고 ID (간접 참조) |
| `amount` | Integer | Not Null, Unupdatable | 변동 수량 (+/-) |
| `snapshotQuantity` | Integer | Not Null, Unupdatable | **변동 직후 물리 재고 수량 (스냅샷) |
| `snapshotAllocatedQuantity` | Integer | Not Null, Unupdatable | 변동 직후 선점 재고 수량 (스냅샷) |
| `type` | StockTransactionType | Not Null, Unupdatable | 변동 유형 Enum (`INCOMING`, `RESERVE`, `ADJUST` 등) |
| `reasonDetail` | String | Nullable, Unupdatable | 관리자 수동 조정 시 상세 사유 |
| `orderId` | Long | Nullable, Unupdatable | 연관 주문 ID (자동 적재 시 사용) |
| `createdBy` | String | Not Null, Unupdatable | 작업 주체 (`"SYSTEM"`, `"ADMIN_KIM"` 등) |
| `createdAt` | LocalDateTime | Not Null, Unupdatable | 기록 생성 일시 (생성 시점 자동 할당) |

### 3.StockService (비즈니스 서비스 레이어)
> 재고 상태 변동 및 주문/관리자 연관 처리를 수행하며, 모든 작업에 대한 이력(StockHistory)을 적재합니다.

| 메서드명 | 파라미터 | 주요 역할 및 설명 |
| :--- | :--- | :--- |
| `increaseStock` | productId, amount | 일반 입고 (주문 연관 없음, `orderId=null` 오버로딩 위임) |
| `increaseStock` | productId, amount, orderId | 주문 연관 입고 (`INCOMING` 이력 적재) |
| `increaseAndReserveStock` | productId, amount, orderId | 예약 주문건 입고 (`quantity`, `allocatedQuantity` 동시 증가) |
| `cancelReservation` | productId, amount, orderId | 출고 전 취소 (`release` 호출, `CANCEL` 이력 적재) |
| `reserve` | productId, amount, orderId | 주문 선점 (가용 재고 확인 후 `RESERVE` 이력 적재) |
| `decrease` | productId, amount, orderId | 출고 확정 (`decrease` 호출, `DECREASE` 이력 적재) |
| `adjust` | productId, newQuantity, reasonDetail, adminUser | 수동 조정 (차이값 `amountDiff` 계산 및 `ADJUST` 이력 적재) |

---

## Error Handling & Exception Strategy

> 비즈니스 예외는 도메인 전용 예외 클래스인 `BusinessException`과 `ErrorCode` Enum을 사용하여 일관된 규격으로 관리하며, `GlobalExceptionHandler`를 통해 `ErrorResponse` DTO 구조로 공통 응답합니다.

### ErrorCode Specification
| ErrorCode | HttpStatus | Code | Error Message | 발생 조건 |
| :--- | :--- | :--- | :--- | :--- |
| `STOCK_NOT_FOUND` | 404 NOT_FOUND | S001 | 등록되지 않은 재고입니다. 등록을 먼저 진행해주세요. | 미등록 재고 대상 수량 변경 시도 시 (`getStockOrThrow`) |
| `STOCK_ALREADY_EXISTS` | 400 BAD_REQUEST | S002 | 이미 해당 창고에 등록된 재고가 존재합니다. | 동일 창고/상품 조합으로 중복 재고 생성 시도 시 |
| `NOT_ENOUGH_STOCK` | 400 BAD_REQUEST | S003 | 가용 재고 수량이 부족합니다. | 선점/출고 시 가용 재고(`quantity - allocatedQuantity`) 초과 시 |
| `INVALID_STOCK_AMOUNT` | 400 BAD_REQUEST | S004 | 재고 수량은 0보다 커야 합니다. | 0 이하 수량 입력 또는 선점 수량 미만으로 수동 조정 시 |
| `INVALID_INPUT_VALUE` | 400 BAD_REQUEST | C001 | 입력값이 올바르지 않습니다. | `@Valid` 검증 실패 또는 필수 값 누락 시 |
| `INVALID_TYPE_VALUE` | 400 BAD_REQUEST | C002 | 잘못된 타입의 값이 전달되었습니다. | 컨트롤러 파라미터 타입 불일치 등 예외 발생 시 |
* **글로벌 파이프라인 핸들링:** 컨트롤러 레이어의 `@Valid` 바인딩 실패(`MethodArgumentNotValidException`) 및 타입 변환 오류(`MethodArgumentTypeMismatchException`)는 각각 `C001(INVALID_INPUT_VALUE)`, `C002(INVALID_TYPE_VALUE)`로 자동 캡처되어 동일한 `ErrorResponse` 규격으로 변환됩니다.
* **입력값 검증 예외 (`IllegalArgumentException`):** `adjust` 수행 시 필수 인자인 조정 사유(`reasonDetail`)가 누락되거나 공백(`isBlank()`)인 경우 발생합니다.

---

## Test Strategy & Troubleshooting

### 1. 도메인 불변식 검증 및 계층형 테스트 설계
* **Problem**: 단순 재고 차감 구조는 동시성 발생 시 재고 왜곡 위험이 크며, 주문 취소 시 수량 원복 로직의 복잡도가 높아지는 구조적 한계 존재.
* **Action**: JUnit 5 `@Nested` 계층 구조를 활용해 입고/선점/해제/출고/조정 등 상태 transition별 테스트 시나리오를 격리. 출고 진행 시 물리 재고와 선점 재고가 동시 차감되어 가용 재고 불변식이 유지됨을 검증.
* **Result**: 결제 대기 상태와 물리적 출고 상태를 격리하여 시스템 정합성 및 예외 발생 시 복구 안정성 확보.

### 2. 가용 재고 계산 기반의 초과 판매(Overselling) 방어
* **Problem**: 결제 진입 시 실물 재고를 즉시 감축할 경우, 결제 실패/취소 시 원복 트랜잭션 비용과 동시성 충돌 발생 위험 증가.
* **Action**: `quantity - allocatedQuantity < requestAmount` 조건을 도메인 1차 방어선으로 수립하고, 가용 재고를 초과하는 주문 요청 시 `IllegalArgumentException`을 즉시 발생시키는 예외 검증 테스트 케이스 작성.
* **Result**: DB Lock 접근 전 도메인 엔티티 자체에서 초과 판매 가능성을 100% 차단하여 불필요한 I/O 부하 방지.

### 3. 동적 파라미터 예외 메시지 검증 방식을 통한 테스트 취약성(Flakiness) 개선
* **Problem**: 동적 수치(`%d`)가 포함된 예외 메시지를 전체 문자열 일치(hasMessage) 방식으로 검증할 때, 메시지 포맷 변경 시 단위 테스트가 쉽게 깨지는 결합도 문제 발생.
* **Action**: AssertJ의 `hasMessageContaining()` 및 `hasMessageStartingWith()` 패턴으로 검증 방식을 전환하여 동적 변화 요소와 고정 비즈니스 예외 문구를 분리.
* **Result**: 비즈니스 도메인의 핵심 예외 사유는 정확하게 검증하면서도, 포맷팅 변경에는 영향받지 않는 견고한 테스트 코드 유지보수성 확보.