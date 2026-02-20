package com.project.hanspoon.common.payment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;  // 연결된 결제 ID

    @Column(name = "product_id")
    private Long productId;  // 구매한 상품 ID (상품인 경우)

    @Column(name = "class_id")
    private Long classId;  // 신청한 클래스 ID (클래스인 경우)

    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 1;  // 수량

    // ========== 편의 메소드 ==========

    /**
     * 상품 주문 항목 생성
     */
    public static PaymentItem createForProduct(Long productId, int quantity) {
        return PaymentItem.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
    }

    /**
     * 클래스 주문 항목 생성
     */
    public static PaymentItem createForClass(Long classId, int quantity) {
        return PaymentItem.builder()
                .classId(classId)
                .quantity(quantity)
                .build();
    }
}
