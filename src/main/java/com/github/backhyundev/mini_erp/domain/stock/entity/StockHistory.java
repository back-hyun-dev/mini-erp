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
    private Long snapshotQuantity; // 변동 직후 최종 잔여 재고 수량 (스냅샷)

    @Column(nullable = false)
    private Long snapshotAllocatedQuantity; // 변동 직후 선점 재고 수량 (스냅샷)

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

    private StockHistory(Long stockId, Long amount, Long snapshotQuantity, StockTransactionType type, String reasonDetail, Long orderId, String createdBy) {
        this.stockId = stockId;
        this.amount = amount;
        this.snapshotQuantity = snapshotQuantity;
        this.type = type;
        this.reasonDetail = reasonDetail;
        this.orderId = orderId;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now(); // 생성 시점 시간 자동 할당
    }

    // 편의 팩토리 메서드: 일반 시스템 자동 적재용 (상세 사유 없음)
    public static StockHistory createAutoHistory(
            Long stockId,
            Long amount,
            Long snapshotQuantity,
            Long snapshotAllocatedQuantity,
            StockTransactionType type,
            Long orderId,
            String createdBy
    ) {
        return new StockHistory(
                stockId,
                amount,
                snapshotQuantity,
                snapshotAllocatedQuantity,
                type,
                null,
                orderId,
                createdBy
        );
    }

    // 편의 팩토리 메서드: 관리자 수동 조정용 (상세 사유 포함)
    public static StockHistory createManualHistory(
            Long stockId,
            Long amount,
            Long snapshotQuantity,
            Long snapshotAllocatedQuantity,
            StockTransactionType type,
            String reasonDetail,
            String createdBy
    ) {
        return new StockHistory(
                stockId,
                amount,
                snapshotQuantity,
                snapshotAllocatedQuantity,
                type,
                reasonDetail,
                null,
                createdBy
        );
    }
}
