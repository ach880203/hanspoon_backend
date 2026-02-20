package com.project.hanspoon.shop.cart.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponseDto {
    private Long itemId;

    private Long productId;
    private String name;
    private int price;
    private int quantity;

    private int lineTotal;      // price * quantity
    private String thumbnailUrl; // 대표 썸네일
}
