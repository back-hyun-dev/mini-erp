package com.github.backhyundev.mini_erp.domain.stock.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stock_history")
@Immutable
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long stockId;

    @Column(nullable = false)
    private Long amount; // 변동 수량 (+5, -2 등)

    @Column(nullable = false)
    private Long beforeQuantity; // 변동 전 잔여 재고 (기존 재고 수량)

    @Column(nullable = false)
    private Long afterQuantity; // 변동 후 잔여 재고 (최종 실사 수량)

    // 선점 재고 스냅샷
    @Column(nullable = false)
    private Long beforeAllocatedQuantity;

    @Column(nullable = false)
    private Long afterAllocatedQuantity;

    // 1. 큰 범주의 변동 사유 (시스템 자동 지정)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockTransactionType type;

    // 2. 관리자 수동 조정 시 상세 사유 (선택 사항: 예 - "운송 중 파손으로 폐기")
    private String reasonDetail;

    private Long orderId;

    @Column(nullable = false)
    private String createdBy; // "USER_102", "ADMIN_KIM", "SYSTEM"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 생성자
    private StockHistory(
            Long stockId,
            Long amount,
            Long beforeQuantity,
            Long afterQuantity,
            Long beforeAllocatedQuantity,
            Long afterAllocatedQuantity,
            StockTransactionType type,
            String reasonDetail,
            Long orderId,
            String createdBy
    ) {
        this.stockId = stockId;
        this.amount = amount;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.beforeAllocatedQuantity = beforeAllocatedQuantity;
        this.afterAllocatedQuantity = afterAllocatedQuantity;
        this.type = type;
        this.reasonDetail = reasonDetail;
        this.orderId = orderId;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    // 1. 재고 최초 등록(CREATE)
    public static StockHistory createInitHistory(Stock stock, String createdBy) {
        return new StockHistory(
                stock.getId(),
                stock.getQuantity(),            // amount
                0L,                             // beforeQuantity (최초는 0)
                stock.getQuantity(),            // afterQuantity
                0L,                             // beforeAllocated
                stock.getAllocatedQuantity(),   // afterAllocated (0)
                StockTransactionType.CREATE,
                null,
                null,
                createdBy
        );
    }

    // 편의 팩토리 메서드: 일반 시스템 자동 적재용 (입출고, 선점, 취소 공통)
    public static StockHistory createAutoHistory(
            Stock stock,
            Long amount,
            Long beforeQuantity,
            Long beforeAllocatedQuantity,
            StockTransactionType type,
            Long orderId,
            String createdBy
    ) {
        return new StockHistory(
                stock.getId(),
                amount,
                beforeQuantity,                 // 변동 전 물리 재고
                stock.getQuantity(),            // 변동 후 물리 재고
                beforeAllocatedQuantity,        // 변동 전 선점 재고
                stock.getAllocatedQuantity(),   // 변동 후 선점 재고
                type,
                null,                           // 자동 적재는 상세 사유 없음
                orderId,
                createdBy
        );
    }

    // 편의 팩토리 메서드: 관리자 수동 실사 조정용 (상세 사유 포함)
    public static StockHistory createManualHistory(
            Stock stock,
            Long amountDiff,
            Long beforeQuantity,            // 역산하지 않고 서비스 단에서 직접 전달받음
            StockTransactionType type,
            String reasonDetail,
            String createdBy
    ) {
        return new StockHistory(
                stock.getId(),
                amountDiff,                     // 변동차 (+3, -2 등)
                beforeQuantity,                 // 조정 전 기존 재고 수량
                stock.getQuantity(),            // 조정 후 최종 재고 수량
                stock.getAllocatedQuantity(),   // 선점 재고 변동 전 (고정)
                stock.getAllocatedQuantity(),   // 선점 재고 변동 후 (고정)
                type,
                reasonDetail,                   // 관리자 입력 사유
                null,                           // 수동 조정은 orderId 없음
                createdBy
        );
    }
}
