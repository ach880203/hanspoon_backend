package com.project.hanspoon.oneday.reservation.dto;

import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassReservationResponseDto {
    private Long id;
    private Long sessionId;
    private ReservationStatus status;
    private LocalDateTime holdExpiredAt;
    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
