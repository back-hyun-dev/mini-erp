# 재고 관리 미니 ERP System

Spring Boot와 JPA 기반으로 구축한 미니 ERP 재고 관리 시스템입니다.  
고객 요청이 몰릴 때 발생하는 **동시성 이슈(Race Condition)**를 직접 재현하고, 단계별 락(Lock) 메커니즘을 적용하여 데이터를 안전하게 보호하는 과정을 학습하기 위한 프로젝트입니다.

---

## Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 4.1.1
- **ORM / Database**: Spring Data JPA, H2 Database
- **Build Tool**: Gradle
- **Testing**: JUnit 5, AssertJ

---

## 주요 기능 및 학습 목표
- [x] **기본 재고 관리**: Stock 엔티티 및 재고 차감(`decrease`) 비즈니스 로직 작성
- [x] **예외 처리**: JPA `Optional` 기반 안전한 예외 처리(`orElseThrow`)
- [ ] **동시성 이슈 재현**: 100개 스레드 동시 요청 시 발생하는 Race Condition 테스트
- [ ] **동시성 해결**: Pessimistic Lock / Optimistic Lock / Redis Distributed Lock 단계별 비교 적용

---

## 동시성 이슈(Race Condition) 해결 과정
*(테스트 코드를 진행하면서 트러블슈팅 내용을 여기에 기록할 예정입니다)*