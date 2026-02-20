package com.project.hanspoon.common.config;

import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.oneday.coupon.entity.ClassCoupon;
import com.project.hanspoon.oneday.coupon.entity.ClassUserCoupon;
import com.project.hanspoon.oneday.coupon.domain.DiscountType;
import com.project.hanspoon.oneday.coupon.repository.ClassCouponRepository;
import com.project.hanspoon.oneday.coupon.repository.ClassUserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements  CommandLineRunner{

    private final UserRepository userRepository;
    private final ClassCouponRepository couponRepository;
    private final ClassUserCouponRepository userCouponRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. 기본 관리자 계정 생성 또는 업데이트
        User admin = userRepository.findByEmail("admin@example.com").map(existingAdmin -> {
            existingAdmin.setRole("ROLE_ADMIN");
            existingAdmin.setStatus(com.project.hanspoon.common.user.constant.UserStatus.ACTIVE);
            existingAdmin.setSpoonCount(10000); // 1만 포인트 설정
            return userRepository.save(existingAdmin);
        }).orElseGet(() -> {
            User newAdmin = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin1234"))
                    .userName("관리자")
                    .status(com.project.hanspoon.common.user.constant.UserStatus.ACTIVE)
                    .role("ROLE_ADMIN")
                    .spoonCount(10000) // 1만 포인트 설정
                    .isDeleted(false)
                    .build();
            return userRepository.save(newAdmin);
        });
        log.info("관리자 계정 준비 완료: admin@example.com / 10,000 포인트");

        // 2. 관리자용 테스트 쿠폰 생성 및 지급
        ClassCoupon adminCoupon = couponRepository.findByName("관리자 테스트용 10% 할인")
                .orElseGet(() -> couponRepository.save(ClassCoupon.builder()
                        .name("관리자 테스트용 10% 할인")
                        .discountType(DiscountType.PERCENT)
                        .discountValue(10)
                        .validDays(30)
                        .active(true)
                        .build()));

        // 유저에게 해당 쿠폰이 이미 있는지 확인 (사용 안 한 유효한 쿠폰 기준)
        boolean hasCoupon = userCouponRepository.findByUserId(admin.getUserId()).stream()
                .anyMatch(uc -> uc.getCoupon().getId().equals(adminCoupon.getId()) && uc.getUsedAt() == null);

        if (!hasCoupon) {
            ClassUserCoupon issued = ClassUserCoupon.issue(admin.getUserId(), adminCoupon, null, LocalDateTime.now());
            userCouponRepository.save(issued);
            log.info("관리자 계정에 테스트용 10% 할인 쿠폰을 지급했습니다.");
        }
    }
}
