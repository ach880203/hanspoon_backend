package com.project.hanspoon.admin.service;

import com.project.hanspoon.admin.dto.AdminDashboardSummaryDto;
import com.project.hanspoon.common.payment.repository.PaymentRepository;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;
import com.project.hanspoon.oneday.reservation.repository.ClassReservationRepository;
import com.project.hanspoon.shop.constant.OrderStatus;
import com.project.hanspoon.shop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.project.hanspoon.oneday.reservation.domain.ReservationStatus.*;
import static com.project.hanspoon.shop.constant.OrderStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ClassReservationRepository reservationRepository;
    private final UserRepository userRepository;

    public AdminDashboardSummaryDto getDashboardSummary() {
        try {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);
            LocalDateTime yesterdayStart = todayStart.minusDays(1);
            LocalDateTime yesterdayEnd = todayEnd.minusDays(1);

            // 1. 매출 (Payment 기준, PAID 상태)
            long todaySales = paymentRepository.findByPayDateBetween(todayStart, todayEnd).stream()
                    .filter(p -> p.getStatus() == com.project.hanspoon.common.payment.constant.PaymentStatus.PAID)
                    .mapToLong(p -> p.getTotalPrice() != null ? p.getTotalPrice() : 0)
                    .sum();

            long yesterdaySales = paymentRepository.findByPayDateBetween(yesterdayStart, yesterdayEnd).stream()
                    .filter(p -> p.getStatus() == com.project.hanspoon.common.payment.constant.PaymentStatus.PAID)
                    .mapToLong(p -> p.getTotalPrice() != null ? p.getTotalPrice() : 0)
                    .sum();

            // 2. 주문 상태
            long paymentCompleted = 0;
            long preparing = 0;
            long shipping = 0;
            long refundRequested = 0;

            try {
                paymentCompleted = orderRepository.countByStatus(OrderStatus.PAID);
                preparing = orderRepository.countByStatus(OrderStatus.CREATED);
                shipping = orderRepository.countByStatus(OrderStatus.SHIPPED);
                refundRequested = orderRepository.countByStatus(OrderStatus.REFUNDED);
            } catch (Exception e) {
                System.err.println("❌ Error fetching order counts: " + e.getMessage());
                e.printStackTrace();
            }

            // 3. 예약 현황
            long todayReservations = 0;
            long pendingCancel = 0;
            long totalCanceled = 0;

            try {

                // 오늘 시작수업 기준 -> 오늘 생성된 예약 기준으로 변경
                todayReservations = reservationRepository.countBySessionStartAtBetweenAndStatusIn(
                        todayStart, todayEnd,
                        List.of(ReservationStatus.PAID, ReservationStatus.COMPLETED,
                                ReservationStatus.CANCELED));

                // Current reservation domain has no explicit CANCEL_REQUESTED state.
                pendingCancel = 0;

                // 전체 취소 건수 (예약 취소 + 기간 만료)
                totalCanceled = reservationRepository.countByStatus(ReservationStatus.CANCELED)
                        + reservationRepository.countByStatus(ReservationStatus.EXPIRED);

                // reservationRepository를 통한 매출 계산 로직은 paymentRepository 합산 방식으로 통합되었으므로 제거

            } catch (Exception e) {
                System.err.println("❌ Error fetching reservation counts: " + e.getMessage());
                e.printStackTrace();
            }

            // 4. CS & 회원
            long newUsersToday = 0;
            long unreadInquiries = 0;

            try {
                newUsersToday = userRepository.countByCreatedAtBetween(todayStart, todayEnd);
            } catch (Exception e) {
                System.err.println("❌ Error fetching user counts: " + e.getMessage());
                e.printStackTrace();
            }

            return AdminDashboardSummaryDto.builder()
                    .sales(AdminDashboardSummaryDto.SalesSummary.builder()
                            .todaySales(todaySales)
                            .yesterdaySales(yesterdaySales)
                            .build())
                    .orders(AdminDashboardSummaryDto.OrderSummary.builder()
                            .paymentCompleted(paymentCompleted)
                            .preparing(preparing)
                            .shipping(shipping)
                            .refundRequested(refundRequested + pendingCancel) // 상품 환불 + 클래스 취소 요청 합산
                            .build())
                    .reservations(AdminDashboardSummaryDto.ReservationSummary.builder()
                            .todayCount(todayReservations)
                            .pendingCancel(pendingCancel)
                            .totalCanceled(totalCanceled)
                            .build())
                    .cs(AdminDashboardSummaryDto.CsSummary.builder()
                            .newUsersToday(newUsersToday)
                            .unreadInquiries(unreadInquiries)
                            .build())
                    .build();
        } catch (Exception e) {
            System.err.println("❌ Critical error in getDashboardSummary: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("대시보드 요약 생성에 실패했습니다.", e);
        }
    }
}
