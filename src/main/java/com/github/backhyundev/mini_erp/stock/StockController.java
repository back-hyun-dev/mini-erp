package com.github.backhyundev.mini_erp.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final PessimisticLockStockService pessimisticLockStockService;
    private final OptimisticLockStockFacade optimisticLockStockFacade;

    // 1. 재고 생성 (공통)
    @PostMapping
    public ResponseEntity<Long> createStock(@RequestParam Long productId, @RequestParam Long quantity) {
        Long stockId = stockService.createStock(productId, quantity);
        return ResponseEntity.ok(stockId);
    }

    // 2. 비관적 락 재고 차감
    @PostMapping("/{id}/decrease/pessimistic")
    public ResponseEntity<Void> decreasePessimistic(@PathVariable Long id, @RequestParam Long quantity) {
        pessimisticLockStockService.decrease(id, quantity);
        return ResponseEntity.ok().build();
    }

    // 3. 낙관적 락 재고 차감
    @PostMapping("/{id}/decrease/optimistic")
    public ResponseEntity<Void> decreaseOptimistic(@PathVariable Long id, @RequestParam Long quantity) throws InterruptedException {
        optimisticLockStockFacade.decrease(id, quantity);
        return ResponseEntity.ok().build();
    }
}