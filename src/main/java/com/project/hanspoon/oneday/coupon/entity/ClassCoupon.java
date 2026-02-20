package com.project.hanspoon.oneday.coupon.entity;

import com.project.hanspoon.common.entity.BaseTimeEntity;
import com.project.hanspoon.oneday.coupon.domain.DiscountType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "class_coupon")
public class ClassCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DiscountType discountType;

    @Column(nullable = false)
    private  int discountValue;

    @Column(nullable = false)
    private int validDays;

    @Column(nullable = false)
    private boolean active;

    @Builder
    private ClassCoupon(String name, DiscountType discountType,
                        int discountValue, int validDays, boolean active) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.validDays = validDays;
        this.active = active;
    }
}
