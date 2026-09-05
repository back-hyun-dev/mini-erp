package com.github.backhyundev.mini_erp.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/{id}/decrease")
    public ResponseEntity<Void> decrease(
            @PathVariable Long id,
            @RequestBody StockDecreaseRequest request
    ) {
        stockService.decreaseWithPessimisticLock(id, request.getQuantity());
        return ResponseEntity.ok().build();
    }
}