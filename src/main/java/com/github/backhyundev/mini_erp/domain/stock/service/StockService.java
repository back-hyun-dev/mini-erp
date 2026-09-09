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
    public void increaseStock(Long productId, Long amount) {
        increaseStock(productId, amount, null);
    }

    // 2. 출고 후 반품 입고 (주문 연관 입고)
    @Transactional
    public void increaseStock(Long productId, Long amount, Long orderId) {
        Stock stock = getStockByProductIdOrThrow(productId);
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
    public void increaseAndReserveStock(Long productId, Long amount, Long orderId) {
        Stock stock = getStockByProductIdOrThrow(productId);

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
    public void cancelReservation(Long productId, Long amount, Long orderId) {
        Stock stock = getStockByProductIdOrThrow(productId);

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
    public void reserve(Long productId, Long amount, Long orderId) {
        Stock stock = getStockByProductIdOrThrow(productId);

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
    public void decrease(Long productId, Long amount, Long orderId) {
        Stock stock = getStockByProductIdOrThrow(productId);

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
    public void adjust(Long productId, Long newQuantity, String reasonDetail, String adminUser) {
        Stock stock = getStockByProductIdOrThrow(productId);

        if (reasonDetail == null || reasonDetail.isBlank()) {
            throw new IllegalArgumentException("재고 조정 시 사유(reasonDetail)는 필수입니다.");
        }

        Long previousQuantity = stock.getQuantity();
        stock.adjust(newQuantity);

        Long amountDiff = newQuantity - previousQuantity;

        recordManualHistory(
                stock.getId(),
                amountDiff,
                stock.getQuantity(),
                stock.getAllocatedQuantity(),
                StockTransactionType.ADJUST,
                reasonDetail,
                adminUser
        );
    }

    // Helper 메서드 (IllegalArgumentException -> BusinessException + ErrorCode로 변경)
    private Stock getStockByProductIdOrThrow(Long productId) {
        return stockRepository.findByProductId(productId)
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