package com.project.hanspoon.common.payment.entity;

import com.project.hanspoon.common.payment.constant.PaymentStatus;
import com.project.hanspoon.common.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_id")
    private Long payId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 결제 회원

    @Column(name = "total_price")
    private Integer totalPrice; // 총 결제 합계액

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PAID; // 상태 (결제완료, 취소)

    @CreationTimestamp
    @Column(name = "pay_date")
    private LocalDateTime payDate; // 결제 승인 일시

    @Column(name = "portone_payment_id")
    private String portOnePaymentId; // 포트원 고유 결제 ID (환불용)

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PaymentItem> paymentItems = new ArrayList<>(); // 주문 항목 목록

    // ========== 비즈니스 메소드 ==========

    /**
     * 주문 항목 추가
     */
    public void addPaymentItem(PaymentItem paymentItem) {
        this.paymentItems.add(paymentItem);
        paymentItem.setPayment(this);
    }

    /**
     * 결제 취소
     */
    public boolean cancelPayment() {
        if (this.status == PaymentStatus.CANCELLED) {
            return false; // 이미 취소된 결제
        }
        this.status = PaymentStatus.CANCELLED;
        return true;
    }
}
