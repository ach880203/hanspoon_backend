package com.project.hanspoon.oneday.coupon.repository;

import com.project.hanspoon.oneday.coupon.entity.ClassUserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassUserCouponRepository extends JpaRepository<ClassUserCoupon, Long> {
    List<ClassUserCoupon> findByUserId(Long userId);

    boolean existsByReservationId(Long reservationId);

    List<ClassUserCoupon> findAllByUserIdOrderByIssuedAtDesc(Long userId);
}
