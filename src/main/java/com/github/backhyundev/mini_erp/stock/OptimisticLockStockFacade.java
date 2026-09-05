package com.github.backhyundev.mini_erp.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OptimisticLockStockFacade {

    private final OptimisticLockStockService optimisticLockStockService;

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true) {
            try {
                // 재고 차감 시도 (트랜잭션 실행)
                optimisticLockStockService.decrease(id, quantity);

                // 성공 시 무한 루프 탈출
                break;
            } catch (Exception e) {
                // 충돌 발생 시 DB 부하를 줄이기 위해 50ms 대기 후 재시도
                Thread.sleep(50);
            }
        }
    }
}