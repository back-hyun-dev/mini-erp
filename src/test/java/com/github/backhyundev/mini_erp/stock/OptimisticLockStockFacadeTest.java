package com.github.backhyundev.mini_erp.stock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OptimisticLockStockFacadeTest {

    @Autowired
    private OptimisticLockStockFacade optimisticLockStockFacade;

    @Autowired
    private StockRepository stockRepository;

    private Stock stock;

    @BeforeEach
    void setUp() {
        // 테스트 전: 재고 100개 세팅 (id: 1L, quantity: 100L)
        stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    void tearDown() {
        // 테스트 후: 데이터 정리
        stockRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("동시에 100개의 재고 차감 요청이 들어와도 낙관적 락 재시도를 통해 재고가 정확히 0이 된다.")
    void decrease_100_requests_concurrently() throws InterruptedException {
        int threadCount = 100;

        // 1. 32개의 쓰레드 풀 생성 (비동기 작업을 처리할 작업자들)
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 2. 100개의 작업이 모두 끝날 때까지 대기하기 위한 Latch (카운트다운: 100)
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 3. 100개의 요청을 비동기로 동시 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 손에 (1L, 1L) 변수를 쥐고 Facade의 while 재시도 로직 호출!
                    optimisticLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    // 작업이 하나 끝날 때마다 Latch 숫자를 -1 줄임
                    latch.countDown();
                }
            });
        }

        // 4. 100개 쓰레드의 차감 작업이 모두 완료될 때까지 메인 쓰레드가 대기
        latch.await();

        // 5. 검증: DB에서 최신 재고 데이터를 가져와 수량이 0인지 확인
        Stock updatedStock = stockRepository.findById(1L).orElseThrow();

        // 재고가 정확히 0개이어야 성공!
        assertThat(updatedStock.getQuantity()).isEqualTo(0L);
    }
}
