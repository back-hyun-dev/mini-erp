package com.github.backhyundev.mini_erp.stock;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StockDecreaseRequest {

    private Long quantity;

    public StockDecreaseRequest(Long quantity) {
        this.quantity = quantity;
    }
}