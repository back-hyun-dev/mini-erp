package com.github.backhyundev.mini_erp.domain.stock.repository;

import com.github.backhyundev.mini_erp.domain.stock.entity.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    // 1. 최근 N개 조회 (Limit) -> Pageable이나 @Query 활용으로 함수명 단축!
    @Query("SELECT h FROM StockHistory h WHERE h.stockId = :stockId ORDER BY h.id DESC")
    List<StockHistory> findRecentByStockId(@Param("stockId") Long stockId, Pageable pageable);

    // 2. 주문 ID로 조회
    List<StockHistory> findByOrderId(Long orderId);

    // 3. 전체 페이징 조회
    Page<StockHistory> findByStockId(Long stockId, Pageable pageable);
}