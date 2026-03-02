package com.project.hanspoon.shop.constant;

public enum OrderStatus {
    CREATED("주문생성"),
    PAID("결제완료"),
    SHIPPED("배송중"),
    DELIVERED("배송완료"),
    CONFIRMED("구매확정"),
    CANCELED("주문취소"), // 결제 전 취소
    REFUNDED("환불완료"); // 결제 후 취소(환불)

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
