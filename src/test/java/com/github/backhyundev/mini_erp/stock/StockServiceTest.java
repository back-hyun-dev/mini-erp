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
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void before() {
        // 테스트 전 ID 1L, 재고 100개 세팅
        Stock stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    void after() {
        stockRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 100개의 요청이 들어올 때 재고 차감 테스트")
    void decrease_100_requests_concurrently() throws InterruptedException {
        int threadCount = 100;
        // 32개의 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        // 100개의 스레드가 모두 끝날 때까지 대기하도록 돕는 Latch
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decreaseWithPessimisticLock(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 100개 작업이 모두 끝날 때까지 메인 스레드 대기

        Stock stock = stockRepository.findById(1L).orElseThrow();

        // 예상되는 잔여 재고: 100 - 100 = 0개
        System.out.println("====== 최종 남은 재고: " + stock.getQuantity() + " ======");
        assertThat(stock.getQuantity()).isEqualTo(0L);
    }
}
