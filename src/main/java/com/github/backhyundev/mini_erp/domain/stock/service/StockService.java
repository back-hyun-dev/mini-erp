package com.github.backhyundev.mini_erp.domain.stock.service;

import com.github.backhyundev.mini_erp.domain.stock.entity.Stock;
import com.github.backhyundev.mini_erp.domain.stock.entity.StockHistory;
import com.github.backhyundev.mini_erp.domain.stock.entity.StockTransactionType;
import com.github.backhyundev.mini_erp.domain.stock.repository.StockHistoryRepository;
import com.github.backhyundev.mini_erp.domain.stock.repository.StockRepository;
import com.github.backhyundev.mini_erp.global.error.BusinessException;
import com.github.backhyundev.mini_erp.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private static final String DEFAULT_SYSTEM_USER = "SYSTEM";

    // 1. 단순 일반 입고 (주문/선점 전혀 상관없는 입고)
    @Transactional
    public void increaseStock(Long warehouseId, Long productId, Long amount) {
        increaseStock(warehouseId, productId, amount, null);
    }

    // 2. 출고 후 반품 입고 (주문 연관 입고)
    @Transactional
    public void increaseStock(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);
        stock.increase(amount);

        recordHistory(
                stock.getId(),
                amount,
                stock.getQuantity(),
                stock.getAllocatedQuantity(),
                StockTransactionType.INCOMING,
                orderId,
                DEFAULT_SYSTEM_USER
        );
    }

    // 3. 예약 주문건 입고 (입고되자마자 선점 묶음)
    @Transactional
    public void increaseAndReserveStock(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        stock.increase(amount); // quantity 증가
        stock.reserve(amount);  // allocatedQuantity 증가

        recordHistory(
                stock.getId(),
                amount,
                stock.getQuantity(),
                stock.getAllocatedQuantity(),
                StockTransactionType.INCOMING,
                orderId,
                DEFAULT_SYSTEM_USER
        );
    }

    // 4. 출고 전 취소 (선점 해제)
    @Transactional
    public void cancelReservation(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        stock.release(amount); // allocatedQuantity 차감

        recordHistory(
                stock.getId(),
                amount,
                stock.getQuantity(),
                stock.getAllocatedQuantity(),
                StockTransactionType.CANCEL,
                orderId,
                DEFAULT_SYSTEM_USER
        );
    }

    // 5. 주문 선점
    @Transactional
    public void reserve(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        stock.reserve(amount);

        recordHistory(
                stock.getId(),
                amount,
                stock.getQuantity(),
                stock.getAllocatedQuantity(),
                StockTransactionType.RESERVE,
                orderId,
                DEFAULT_SYSTEM_USER
        );
    }

    // 6. 출고 확정
    @Transactional
    public void decrease(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        stock.decrease(amount);

        recordHistory(
                stock.getId(),
                amount,
                stock.getQuantity(),
                stock.getAllocatedQuantity(),
                StockTransactionType.DECREASE,
                orderId,
                DEFAULT_SYSTEM_USER
        );
    }

    // 7. 관리자가 조정 (파손 / 분실 등)
    @Transactional
    public void adjust(Long warehouseId, Long productId, Long newQuantity, String reasonDetail, String adminUser) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        if (reasonDetail == null || reasonDetail.isBlank()) {
            throw new IllegalArgumentException("재고 조정 시 사유(reasonDetail)는 필수입니다.");
        }

        // 1. 조정 전 상태 스냅샷 미리 보관
        Long previousQuantity = stock.getQuantity();
        Long previousAllocatedQuantity = stock.getAllocatedQuantity();

        // 2. 수량 조정
        stock.adjust(newQuantity);

        // 3. 변동 차이값 계산
        Long amountDiff = newQuantity - previousQuantity;

        // 4. 조정 전 스냅샷 값을 히스토리에 적재
        recordManualHistory(
                stock.getId(),
                amountDiff,
                previousQuantity,
                previousAllocatedQuantity,
                StockTransactionType.ADJUST,
                reasonDetail,
                adminUser
        );
    }

    // Helper 메서드 (창고 ID + 상품 ID 조합 조회)
    private Stock getStockOrThrow(Long warehouseId, Long productId) {
        return stockRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
    }

    // 1. 기본 히스토리 적재 (자동)
    private void recordHistory(
            Long stockId,
            Long amount,
            Long snapshotQuantity,
            Long snapshotAllocatedQuantity,
            StockTransactionType type,
            Long orderId,
            String createdBy
    ) {
        StockHistory history = StockHistory.createAutoHistory(
                stockId,
                amount,
                snapshotQuantity,
                snapshotAllocatedQuantity,
                type,
                orderId,
                createdBy
        );
        stockHistoryRepository.save(history);
    }

    // 2. 수동 조정 히스토리 적재 (조정)
    private void recordManualHistory(
            Long stockId,
            Long amount,
            Long snapshotQuantity,
            Long snapshotAllocatedQuantity,
            StockTransactionType type,
            String reasonDetail,
            String createdBy
    ) {
        StockHistory history = StockHistory.createManualHistory(
                stockId,
                amount,
                snapshotQuantity,
                snapshotAllocatedQuantity,
                type,
                reasonDetail,
                createdBy
        );
        stockHistoryRepository.save(history);
    }
}