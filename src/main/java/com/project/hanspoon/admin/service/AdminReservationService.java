package com.project.hanspoon.admin.service;

import com.project.hanspoon.admin.dto.AdminReservationItemDto;
import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.oneday.coupon.repository.ClassUserCouponRepository;
import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;
import com.project.hanspoon.oneday.reservation.entity.ClassReservation;
import com.project.hanspoon.oneday.reservation.repository.ClassReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReservationService {

    private final ClassReservationRepository reservationRepository;
    private final ClassUserCouponRepository classUserCouponRepository;

    /**
     * 관리자 예약 목록 조회
     *
     * status 파라미터 규칙:
     * - null/blank/ALL: 전체
     * - HOLD/PAID/CANCELED/EXPIRED/COMPLETED: 해당 상태만
     * - CANCEL_REQUESTED: 현재 예약 상태 enum에 없어 빈 목록
     */
    public List<AdminReservationItemDto> getReservations(String status) {
        if ("CANCEL_REQUESTED".equalsIgnoreCase(trim(status))) {
            return List.of();
        }

        List<ClassReservation> rows;
        ReservationStatus parsed = parseStatus(status);
        if (parsed == null) {
            rows = reservationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            rows = reservationRepository.findByStatus(parsed).stream()
                    .sorted(Comparator.comparing(ClassReservation::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }

        return rows.stream().map(this::toDto).toList();
    }

    /**
     * 취소 요청 목록 조회
     * 현재 예약 상태 모델에 CANCEL_REQUESTED 상태가 없어서 임시로 빈 목록입니다.
     */
    public List<AdminReservationItemDto> getCancelRequests() {
        return List.of();
    }

    /**
     * 취소 승인/거절 플로우는 현재 예약 상태 모델상 존재하지 않습니다.
     * 프론트 계약 경로 유지를 위해 명확한 안내 메시지를 반환합니다.
     */
    @Transactional
    public void approveCancel(Long reservationId) {
        throw new BusinessException("현재 시스템은 취소 승인 플로우(CANCEL_REQUESTED)를 사용하지 않습니다.");
    }

    @Transactional
    public void rejectCancel(Long reservationId) {
        throw new BusinessException("현재 시스템은 취소 거절 플로우(CANCEL_REQUESTED)를 사용하지 않습니다.");
    }

    private AdminReservationItemDto toDto(ClassReservation r) {
        var session = r.getSession();
        var clazz = (session != null ? session.getClassProduct() : null);
        var user = r.getUser();

        boolean paymentCompleted = r.getStatus() == ReservationStatus.PAID || r.getStatus() == ReservationStatus.COMPLETED;
        boolean couponIssued = classUserCouponRepository.existsByReservationId(r.getId());

        return new AdminReservationItemDto(
                r.getId(),
                r.getStatus() != null ? r.getStatus().getDescription() : "-",
                r.getStatus() != null ? r.getStatus().name() : "-",
                clazz != null ? clazz.getId() : null,
                session != null ? session.getId() : null,
                clazz != null ? clazz.getTitle() : null,
                session != null ? session.getPrice() : null,
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCanceledAt(),
                user != null ? user.getUserId() : null,
                user != null ? user.getUserName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                session != null ? session.getStartAt() : null,
                r.getHoldExpiredAt(),
                r.getPaidAt(),
                paymentCompleted,
                couponIssued,
                null
        );
    }

    private ReservationStatus parseStatus(String status) {
        String normalized = trim(status);
        if (normalized == null || normalized.isBlank() || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        try {
            return ReservationStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("지원하지 않는 예약 상태입니다: " + status);
        }
    }

    private String trim(String v) {
        return v == null ? null : v.trim();
    }
}
