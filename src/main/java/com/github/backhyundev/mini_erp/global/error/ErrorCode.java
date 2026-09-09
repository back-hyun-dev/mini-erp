package com.github.backhyundev.mini_erp.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Stock
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "등록되지 않은 재고입니다. 등록을 먼저 진행해주세요."),
    STOCK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "S002", "이미 해당 창고에 등록된 재고가 존재합니다."),
    NOT_ENOUGH_STOCK(HttpStatus.BAD_REQUEST, "S003", "가용 재고 수량이 부족합니다."),
    INVALID_STOCK_AMOUNT(HttpStatus.BAD_REQUEST, "S004", "재고 수량은 0보다 커야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
