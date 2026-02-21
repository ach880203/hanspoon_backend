package com.project.hanspoon.oneday.reservation.service;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.oneday.clazz.entity.ClassSession;
import com.project.hanspoon.oneday.clazz.repository.ClassSessionRepository;
import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;
import com.project.hanspoon.oneday.reservation.dto.ReservationResponse;
import com.project.hanspoon.oneday.reservation.entity.ClassReservation;
import com.project.hanspoon.oneday.reservation.repository.ClassReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassReservationService {

    private static final int HOLD_MINUTES = 10;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final ClassSessionRepository classSessionRepository;
    private final ClassReservationRepository reservationRepository;
    private final UserRepository userRepository;

    public ReservationResponse createHold(Long sessionId, Long userId) {
        validateUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다. id=" + userId));

        // 동시 예약 경쟁에서 정원 초과를 막기 위해 세션을 락으로 조회합니다.
        ClassSession session = classSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException("세션을 찾을 수 없습니다. id=" + sessionId));

        // 서버의 KST 시간 기준으로 이미 시작한 수업은 예약하지 못하게 막습니다.
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        if (!session.getStartAt().isAfter(now)) {
            throw new BusinessException("이미 시작된 수업은 예약할 수 없습니다.");
        }

        List<ClassReservation> existingReservations = reservationRepository.findBySession_IdAndUser_UserId(sessionId,
                userId);
        for (ClassReservation res : existingReservations) {
            if (res.getStatus() == ReservationStatus.PAID) {
                throw new BusinessException("이미 결제 완료된 세션입니다.");
            }
            if (res.getStatus() == ReservationStatus.HOLD && !res.isExpired(now)) {
                return ReservationResponse.from(res); // 이미 유효한 홀드가 있으면 반환
            }
        }

        if (session.remainingSeats() <= 0) {
            throw new BusinessException("정원이 마감되었습니다.");
        }

        // 엔티티 메서드에서도 마지막으로 좌석 상태를 점검합니다.
        try {
            session.increaseReserved();
        } catch (IllegalStateException ex) {
            throw new BusinessException("정원이 마감되었습니다.");
        }

        ClassReservation reservation = ClassReservation.builder()
                .session(session)
                .user(user)
                .status(ReservationStatus.HOLD)
                .holdExpiredAt(now.plusMinutes(HOLD_MINUTES))
                .build();

        ClassReservation saved = reservationRepository.save(reservation);
        return ReservationResponse.from(saved);
    }

    // 레거시 pay 메소드 제거됨. 결제 확정은 PortOneService.verifyAndSavePayment를 사용하세요.

    public ReservationResponse cancel(Long reservationId, Long userId) {
        validateUserId(userId);

        ClassReservation reservation = reservationRepository.findByIdAndUserIdForUpdate(reservationId, userId)
                .orElseThrow(() -> new BusinessException("예약을 찾을 수 없습니다. id=" + reservationId));

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new BusinessException("이미 취소된 예약입니다.");
        }
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new BusinessException("만료된 예약입니다.");
        }

        ClassSession session = classSessionRepository.findByIdForUpdate(reservation.getSession().getId())
                .orElseThrow(() -> new BusinessException("세션을 찾을 수 없습니다."));

        session.decreaseReserved();
        reservation.markCanceled(LocalDateTime.now(KST_ZONE));
        return ReservationResponse.from(reservation);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("로그인 정보가 올바르지 않습니다.");
        }
    }
}
