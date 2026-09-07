# 재고 관리 미니 ERP System

Spring Boot와 JPA 기반으로 구축한 재고 관리 및 동시성 제어 엔진입니다.  
고객 요청이 몰릴 때 발생하는 **동시성 이슈(Race Condition)** 와 **초과판매(Overselling)** 문제를 방지하기 위해, 도메인 불변식 수립 및 JPA 낙관적 락(Optimistic Lock) 기반의 방어 체계를 구축했습니다.

---

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **ORM / Database**: Spring Data JPA, mySQL
- **Build Tool**: Gradle
- **Testing**: JUnit 5, AssertJ

---

## 주요 기능 및 핵심 설계
- **3단계 재고 라이프사이클 관리**: 결제 시점 선점(reserve), 물류 출고 차감(decrease), 재물조사 수량 조정(adjust)
- **도메인 정합성 보장**: JPA Optional 기반 안전한 예외 처리 및 도메인 엔티티 내 불변식 검증
- **동시성 제어 및 Race Condition 방지**: 멀티스레드 환경에서의 재고 차감 정합성 보장

---

## Key Engineering Decisions & Architecture

### 1. 동시성 제어 및 도메인 정합성을 위한 2단계 방어 체계 구축
* **3단계 재고 라이프사이클 분리**: 초과 판매(Overselling) 차단을 위해 물리 재고(quantity)와 선점 재고(allocatedQuantity)를 분리. 결제 시점엔 가용 재고 범위 내 선점(reserve), 물류 출고 시점엔 실제 차감(decrease) 진행.
* **도메인 불변식(Invariant) 보장**: 엔티티 내에 quantity >= allocatedQuantity >= 0 규칙을 최우선으로 수립. 실무 재물조사 조정(adjust) 시 선점 수량보다 적은 수치로 변경 시 예외를 발생시켜 가용 재고 음수 상태를 완벽히 차단.
* **수학적 조건 최적화**: reserve() 시점에 가용 재고 검증이 완료되므로, 출고(decrease) 시에는 선점 수량 검증만 수행. 중복 검사를 제거하여 도메인 메서드의 책임을 단축하고 비즈니스 의도를 명확히 표현.
* **Persistence Layer 동시성 제어**: JPA @Version 기반 낙관적 락(Optimistic Locking)을 적용하여, DB 병목을 최소화하면서 동시 수정 충돌 시 데이터 정합성 보장.

### 2. 대규모 분산 환경을 고려한 식별자(PK) 확장 전략 (YAGNI 원칙)
* **Zero-Cost Abstraction**: 초기 개발 단계에서는 오버 엔지니어링을 지양하고 가벼운 Long (Auto-Increment) 방식을 채택하여 빠른 개발 속도 및 DB 인덱스 성능 확보.
* **Future-Proof Strategy**: PK 규격을 64비트 정수(Long)로 고정함으로써, 향후 분산 DB(샤딩) 환경으로 확장하더라도 DB 스키마 변경 없이 애플리케이션 채번 알고리즘(TSID/Snowflake) 교체만으로 전환 가능하도록 설계.

## Test Strategy & Troubleshooting

### 1. 도메인 불변식 검증 및 계층형 테스트 설계
* **Problem**: 단순 재고 차감 구조는 동시성 발생 시 재고 왜곡 위험이 크며, 주문 취소 시 수량 원복 로직의 복잡도가 높아지는 구조적 한계 존재.
* **Action**: JUnit 5 @Nested 계층 구조를 활용해 입고/선점/해제/출고/조정 등 상태 transition별 테스트 시나리오를 격리. 출고 진행 시 물리 재고와 선점 재고가 동시 차감되어 가용 재고 불변식이 유지됨을 검증.
* **Result**: 결제 대기 상태와 물리적 출고 상태를 격리하여 시스템 정합성 및 예외 발생 시 복구 안정성 확보.

### 2. 가용 재고 계산 기반의 초과 판매(Overselling) 방어
* **Problem**: 결제 진입 시 실물 재고를 즉시 감축할 경우, 결제 실패/취소 시 원복 트랜잭션 비용과 동시성 충돌 발생 위험 증가.
* **Action**: `quantity - allocatedQuantity < requestAmount` 조건을 도메인 1차 방어선으로 수립하고, 가용 재고를 초과하는 주문 요청 시 IllegalArgumentException을 즉시 발생시키는 예외 검증 테스트 케이스 작성.
* **Result**: DB Lock 접근 전 도메인 엔티티 자체에서 초과 판매 가능성을 100% 차단하여 불필요한 I/O 부하 방지.

### 3. 동적 파라미터 예외 메시지 검증 방식을 통한 테스트 취약성(Flakiness) 개선
* **Problem**: 동적 수치(%d)가 포함된 예외 메시지를 전체 문자열 일치(hasMessage) 방식으로 검증할 때, 메시지 포맷 변경 시 단위 테스트가 쉽게 깨지는 결합도 문제 발생.
* **Action**: AssertJ의 hasMessageContaining() 및 hasMessageStartingWith() 패턴으로 검증 방식을 전환하여 동적 변화 요소와 고정 비즈니스 예외 문구를 분리.
* **Result**: 비즈니스 도메인의 핵심 예외 사유는 정확하게 검증하면서도, 포맷팅 변경에는 영향받지 않는 견고한 테스트 코드 유지보수성 확보.
