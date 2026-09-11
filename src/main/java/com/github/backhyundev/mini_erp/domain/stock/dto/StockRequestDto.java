package com.github.backhyundev.mini_erp.domain.stock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockRequestDto {

    // 1. 재고 입고 / 출고 / 선점 / 취소 요청 DTO
    @Getter
    @NoArgsConstructor
    public static class Process {
        @NotNull(message = "창고 ID는 필수입니다.")
        private Long warehouseId;

        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 0보다 커야 합니다.")
        private Long amount;

        private Long orderId; // 입고/선점/출고 시 연관된 주문 ID (선택)
    }

    // 2. 관리자 재고 수동 조정 요청 DTO
    @Getter
    @NoArgsConstructor
    public static class Adjust {
        @NotNull(message = "창고 ID는 필수입니다.")
        private Long warehouseId;

        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @NotNull(message = "조정할 수량은 필수입니다.")
        @PositiveOrZero(message = "조정 수량은 0 이상이어야 합니다.") // 0개 조정(재고 소진)도 가능
        private Long newQuantity;

        @NotBlank(message = "재고 조정 사유는 필수 입력 항목입니다.")
        private String reasonDetail;
    }
}