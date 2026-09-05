package com.github.backhyundev.mini_erp.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OptimisticLockStockService {

    private final StockRepository stockRepository;

    @Transactional
    public void decrease(Long id, Long quantity) {
        // Repository에 만든 findByIdWithOptimisticLock 호출
        Stock stock = stockRepository.findByIdWithOptimisticLock(id);
        stock.decrease(quantity);
    }
}
