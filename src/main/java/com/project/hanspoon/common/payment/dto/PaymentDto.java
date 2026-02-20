package com.project.hanspoon.common.payment.dto;

import com.project.hanspoon.common.payment.constant.PaymentStatus;
import com.project.hanspoon.common.payment.entity.PaymentItem;
import com.project.hanspoon.common.payment.entity.Payment;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {

    private Long payId;
    private Long userId;
    private String userName;
    private String email;
    private Integer totalPrice;
    private PaymentStatus status;
    private LocalDateTime payDate;
    private String orderName;
    private List<PaymentItemDto> paymentItems;

    public static PaymentDto fromEntity(Payment payment) {
        return PaymentDto.builder()
                .payId(payment.getPayId())
                .userId(payment.getUser().getUserId())
                .userName(payment.getUser().getUserName())
                .email(payment.getUser().getEmail())
                .totalPrice(payment.getTotalPrice())
                .status(payment.getStatus())
                .payDate(payment.getPayDate())
                .paymentItems(payment.getPaymentItems().stream()
                        .map(PaymentItemDto::fromEntity)
                        .collect(Collectors.toList()))
                .orderName(payment.getPaymentItems().isEmpty() ? "결제 내역" : 
                           (payment.getPaymentItems().size() > 1 ? 
                            "상품 외 " + (payment.getPaymentItems().size() - 1) + "건" : "결제 상품"))
                .build();
    }

    public static PaymentDto from(Payment payment) {
        return fromEntity(payment);
    }

    public String getStatusText() {
        return status == PaymentStatus.PAID ? "결제완료" : "취소";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentItemDto {
        private Long itemId;
        private Long productId;
        private Long classId;
        private Integer quantity;
        private String itemType;

        public static PaymentItemDto fromEntity(PaymentItem item) {
            return PaymentItemDto.builder()
                    .itemId(item.getId())
                    .productId(item.getProductId())
                    .classId(item.getClassId())
                    .quantity(item.getQuantity())
                    .itemType(item.getProductId() != null ? "상품" : "클래스")
                    .build();
        }
    }
}
