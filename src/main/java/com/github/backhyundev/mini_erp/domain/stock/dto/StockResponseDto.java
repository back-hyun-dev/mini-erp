package com.github.backhyundev.mini_erp.domain.stock.dto;

import com.github.backhyundev.mini_erp.domain.stock.entity.Stock;
import lombok.Getter;

@Getter
public class StockResponseDto {

    private final Long stockId;
    private final Long warehouseId;
    private final Long productId;
    private final Long quantity;          // 전체 물리 재고 수량
    private final Long allocatedQuantity; // 주문 선점(할당) 수량
    private final Long availableQuantity; // 실제 주문/출고 가능한 가용 수량

    // Stock 엔티티 -> DTO 변환용 생성자
    public StockResponseDto(Stock stock) {
        this.stockId = stock.getId();
        this.warehouseId = stock.getWarehouseId();
        this.productId = stock.getProductId();
        this.quantity = stock.getQuantity();
        this.allocatedQuantity = stock.getAllocatedQuantity();
        this.availableQuantity = stock.getAvailableQuantity();
    }

    // 정적 팩토리 메서드
    public static StockResponseDto from(Stock stock) {
        return new StockResponseDto(stock);
    }
}