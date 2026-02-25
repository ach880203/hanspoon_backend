package com.project.hanspoon.admin.dto;

import java.time.LocalDateTime;

/**
 * 愿由ъ옄 ?덉빟 紐⑸줉 移대뱶 1嫄댁쓣 ?쒗쁽?섎뒗 DTO?낅땲??
 *
 * ?꾨줎??AdminReservationList.jsx)媛 洹몃?濡??곕뒗 ?꾨뱶紐낆쑝濡?留욎떠?먯뿀?듬땲??
 * - reservationId, status, classTitle, price, createdAt
 * - userName, userEmail, sessionStart, cancelReason
 */
public record AdminReservationItemDto(
        Long reservationId,
        String status,
        String statusCode,
        Long classId,
        Long sessionId,
        String classTitle,
        Integer price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime canceledAt,
        Long userId,
        String userName,
        String userEmail,
        String userPhone,
        LocalDateTime sessionStart,
        LocalDateTime holdExpiredAt,
        LocalDateTime paidAt,
        boolean paymentCompleted,
        boolean couponIssued,
        String cancelReason
) {
}
