package com.project.hanspoon.admin.dto;

import java.time.LocalDateTime;

/**
 * 관리자 예약 목록 카드 1건을 표현하는 DTO입니다.
 *
 * 프론트(AdminReservationList.jsx)가 그대로 쓰는 필드명으로 맞춰두었습니다.
 * - reservationId, status, classTitle, price, createdAt
 * - userName, userEmail, sessionStart, cancelReason
 */
public record AdminReservationItemDto(
        Long reservationId,
        String status,
        String classTitle,
        Integer price,
        LocalDateTime createdAt,
        String userName,
        String userEmail,
        LocalDateTime sessionStart,
        String cancelReason
) {
}
