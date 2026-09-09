package com.github.backhyundev.mini_erp.domain.stock.entity;

public enum StockTransactionType {
    INCOMING,   // 입고
    RESERVE,    // 결제 선점
    CANCEL,     // 선점 해제 (주문 취소)
    DECREASE,   // 물류 출고 (실제 차감)
    ADJUST      // 재물조사 수량 조정
}