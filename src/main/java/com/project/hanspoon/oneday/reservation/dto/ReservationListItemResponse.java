package com.project.hanspoon.oneday.reservation.dto;

import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationListItemResponse(
        Long reservationId,
        ReservationStatus status,
        LocalDateTime holdExpiredAt,

        Long sessionId,
        LocalDateTime startAt,
        String slot,
        int price,

        Long classId,
        String classTitle
) {}
