package com.project.hanspoon.oneday.reservation.dto;

import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationDetailResponse(
        Long reservationId,
        ReservationStatus status,
        LocalDateTime holdExpiredAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,

        Long sessionId,
        LocalDateTime startAt,
        String slot,
        int capacity,
        int reservedCount,
        int price,

        Long classId,
        String classTitle
) {
}
