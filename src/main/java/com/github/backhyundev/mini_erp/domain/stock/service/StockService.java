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

    @Transactional
    public void registerStock(Long warehouseId, Long productId, Long initialQuantity, String createdBy) {
        // 1. 이미 등록된 재고인지 확인
        if (stockRepository.findByWarehouseIdAndProductId(warehouseId, productId).isPresent()) {
            throw new BusinessException(ErrorCode.STOCK_ALREADY_EXISTS);
        }

        // 2. 명시적인 정적 팩토리 메서드로 안전하게 최초 생성
        Stock stock = Stock.registerInitialStock(warehouseId, productId, initialQuantity);
        Stock savedStock = stockRepository.save(stock);

        // 3. 최초 재고 히스토리 스냅샷 적재 (INIT)
        StockHistory history = StockHistory.createInitHistory(savedStock, createdBy);
        stockHistoryRepository.save(history);
    }

    // 1. 단순 일반 입고 (주문/선점 전혀 상관없는 입고)
    @Transactional
    public void increaseStock(Long warehouseId, Long productId, Long amount) {
        increaseStock(warehouseId, productId, amount, null);
    }

    // 2. 출고 후 반품 입고 (주문 연관 입고)
    @Transactional
    public void increaseStock(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        Long beforeQuantity = stock.getQuantity();
        Long beforeAllocated = stock.getAllocatedQuantity();

        stock.increase(amount);

        StockHistory history = StockHistory.createAutoHistory(
                stock, amount, beforeQuantity, beforeAllocated, StockTransactionType.INCOMING, orderId, DEFAULT_SYSTEM_USER
        );
        stockHistoryRepository.save(history);
    }

    // 3. 예약 주문건 입고 (입고되자마자 선점 묶음)
    @Transactional
    public void increaseAndReserveStock(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        // 1. 초기 스냅샷만 캡처
        Long beforeQuantity = stock.getQuantity();
        Long beforeAllocated = stock.getAllocatedQuantity();

        // 2. 물리 입고
        stock.increase(amount);
        StockHistory movementHistory = StockHistory.createAutoHistory(
                stock, amount, beforeQuantity, beforeAllocated, StockTransactionType.INCOMING, orderId, DEFAULT_SYSTEM_USER
        );

        // 3. 선점 처리 (선점 전 물리수량은 이미 증가된 stock.getQuantity()를 직접 전달)
        stock.reserve(amount);
        StockHistory allocationHistory = StockHistory.createAutoHistory(
                stock, amount, stock.getQuantity(), beforeAllocated, StockTransactionType.RESERVE, orderId, DEFAULT_SYSTEM_USER
        );

        stockHistoryRepository.save(movementHistory);
        stockHistoryRepository.save(allocationHistory);
    }

    // 4. 출고 전 취소 (선점 해제)
    @Transactional
    public void cancelReservation(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        Long beforeQuantity = stock.getQuantity();
        Long beforeAllocated = stock.getAllocatedQuantity();

        stock.release(amount);

        StockHistory history = StockHistory.createAutoHistory(
                stock, -amount, beforeQuantity, beforeAllocated, StockTransactionType.CANCEL, orderId, DEFAULT_SYSTEM_USER
        );
        stockHistoryRepository.save(history);
    }

    // 5. 주문 선점
    @Transactional
    public void reserve(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        Long beforeQuantity = stock.getQuantity();
        Long beforeAllocated = stock.getAllocatedQuantity();

        stock.reserve(amount);

        StockHistory history = StockHistory.createAutoHistory(
                stock, amount, beforeQuantity, beforeAllocated, StockTransactionType.RESERVE, orderId, DEFAULT_SYSTEM_USER
        );
        stockHistoryRepository.save(history);
    }

    // 6. 출고 확정
    @Transactional
    public void decrease(Long warehouseId, Long productId, Long amount, Long orderId) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        Long beforeQuantity = stock.getQuantity();
        Long beforeAllocated = stock.getAllocatedQuantity();

        stock.decrease(amount);

        StockHistory history = StockHistory.createAutoHistory(
                stock, -amount, beforeQuantity, beforeAllocated, StockTransactionType.DECREASE, orderId, DEFAULT_SYSTEM_USER
        );
        stockHistoryRepository.save(history);
    }

    // 7. 관리자가 조정 (파손 / 분실 등)
    @Transactional
    public void adjust(Long warehouseId, Long productId, Long newQuantity, String reasonDetail, String adminUser) {
        Stock stock = getStockOrThrow(warehouseId, productId);

        if (reasonDetail == null || reasonDetail.isBlank()) {
            throw new IllegalArgumentException("재고 조정 시 사유는 필수입니다.");
        }

        Long beforeQuantity = stock.getQuantity();
        stock.adjust(newQuantity);

        Long amountDiff = newQuantity - beforeQuantity;

        StockHistory history = StockHistory.createManualHistory(
                stock,
                amountDiff,
                beforeQuantity,
                StockTransactionType.ADJUST,
                reasonDetail,
                adminUser
        );
        stockHistoryRepository.save(history);
    }

    // Helper 메서드 (창고 ID + 상품 ID 조합 조회)
    private Stock getStockOrThrow(Long warehouseId, Long productId) {
        return stockRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
    }
}