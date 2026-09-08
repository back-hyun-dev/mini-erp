package com.github.backhyundev.mini_erp.domain.stock.repository;

import com.github.backhyundev.mini_erp.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    // Product와 Stock이 분리되어 있으므로, productId로 해당 상품의 Stock을 조회
    Optional<Stock> findByProductId(Long productId);
}