package com.github.backhyundev.mini_erp.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    // 1. 초기 재고 데이터 생성
    @Transactional
    public Long createStock(Long productId, Long quantity) {
        Stock stock = new Stock(productId, quantity);
        Stock savedStock = stockRepository.save(stock);
        return savedStock.getId();
    }

    // 2. 재고 조회 (단순 조회)
    @Transactional(readOnly = true)
    public Stock getStock(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 재고입니다."));
    }
}