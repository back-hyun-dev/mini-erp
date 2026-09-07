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
