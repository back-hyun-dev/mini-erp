package com.github.backhyundev.mini_erp.domain.stock.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "stocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_warehouse_product",
                        columnNames = {"warehouse_id", "product_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long quantity; // 물리 재고

    @Column(name = "allocated_quantity", nullable = false)
    private Long allocatedQuantity; // 선점(할당) 재고

    @Version
    private Long version; // 낙관적 락을 위한 동시성 제어 버전

    // === 생성자 ===
    Stock(Long warehouseId, Long productId, Long initialQuantity) {
        validateNotNull(warehouseId, "창고 ID는 필수입니다.");
        validateNotNull(productId, "상품 ID는 필수입니다.");
        validatePositiveOrZero(initialQuantity, "초기 재고는 0 이상이어야 합니다.");

        this.warehouseId = warehouseId;
        this.productId = productId;
        this.quantity = initialQuantity;
        this.allocatedQuantity = 0L; // 초기 할당 수량은 0
    }

    // === 가용 재고 조회 (핵심 비즈니스 메서드) ===
    public Long getAvailableQuantity() {
        return this.quantity - this.allocatedQuantity;
    }

    // === 1. 단순 입고 / 반품 (물리 재고 증가) ===
    public void increase(Long amount) {
        validatePositiveAmount(amount, "증가시킬 수량은 0보다 커야 합니다.");

        // 수치 오버플로우 방어
        if (Long.MAX_VALUE - amount < this.quantity) {
            throw new IllegalArgumentException("재고 수량이 시스템 처리 한계를 초과했습니다.");
        }

        this.quantity += amount;
    }

    // === 2. 재고 선점 (주문 발생 시) ===
    public void reserve(Long amount) {
        validatePositiveAmount(amount, "선점할 수량은 0보다 커야 합니다.");

        // 가용 재고 부족 검증 (Overselling 차단)
        if (getAvailableQuantity() < amount) {
            throw new IllegalArgumentException(
                    String.format("가용 재고가 부족합니다. (가용 재고: %d, 요청 수량: %d)", getAvailableQuantity(), amount)
            );
        }

        this.allocatedQuantity += amount;
    }

    // === 3. 선점 해제 (주문 취소 시) ===
    public void release(Long amount) {
        validatePositiveAmount(amount, "해제할 수량은 0보다 커야 합니다.");

        if (this.allocatedQuantity < amount) {
            throw new IllegalArgumentException("해제할 선점 재고 수량이 현재 선점된 수량보다 큽니다.");
        }

        this.allocatedQuantity -= amount;
    }

    // === 4. 출고 확정 (물리 출고 처리) ===
    public void decrease(Long amount) {
        validatePositiveAmount(amount, "출고할 수량은 0보다 커야 합니다.");

        // 선점된 재고 및 전체 재고 검증
        if (this.allocatedQuantity < amount) {
            throw new IllegalArgumentException("선점된 재고 수량을 초과하여 출고할 수 없습니다.");
        }

        this.quantity -= amount;
        this.allocatedQuantity -= amount; // 출고가 완료되었으므로 선점 수량도 함께 차감
    }

    // === 5. 재고 실사 조정 (파손/분실/전산 맞춤) ===
    public void adjust(Long targetQuantity) {
        validatePositiveOrZero(targetQuantity, "조정할 재고 수량은 0 이상이어야 합니다.");

        if (targetQuantity < this.allocatedQuantity) {
            throw new IllegalArgumentException(
                    String.format("현재 선점된 주문 수량(%d)보다 적은 수량으로 재고를 조정할 수 없습니다.", this.allocatedQuantity)
            );
        }

        this.quantity = targetQuantity;
    }

    // === Guard Clauses (검증 로직) ===
    private void validateNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePositiveAmount(Long amount, String message) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePositiveOrZero(Long amount, String message) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}