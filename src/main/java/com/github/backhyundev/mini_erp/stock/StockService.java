package com.github.backhyundev.mini_erp.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    // 1. 초기 재고 데이터 생성 (Stock 저장)
    @Transactional
    public Long createStock(Long productId, Long quantity) {
        Stock stock = new Stock(productId, quantity);
        Stock savedStock = stockRepository.save(stock);
        return savedStock.getId();
    }

    // 2. 재고 차감 로직
    @Transactional
    public void decreaseWithPessimisticLock(Long id, Long quantity) {
        // 락이 걸린 조회 메서드 사용
        Stock stock = stockRepository.findByIdWithPessimisticLock(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 재고입니다."));

        stock.decrease(quantity);
    }
}