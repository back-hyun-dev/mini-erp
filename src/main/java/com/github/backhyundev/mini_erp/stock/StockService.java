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
    public void decrease(Long id, Long quantity) {
        // Stock 조회 (없으면 예외 발생)
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 재고입니다. id=" + id));

        // 재고 차감 (Stock 엔티티 내부 메서드 호출)
        stock.decrease(quantity);

        // JPA의 영속성 컨텍스트(Dirty Checking) 덕분에 별도의 save() 호출 없이도 트랜잭션 종료 시 DB에 반영됩니다.
    }
}