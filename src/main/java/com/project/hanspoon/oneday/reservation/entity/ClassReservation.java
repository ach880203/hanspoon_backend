package com.project.hanspoon.oneday.reservation.entity;

import com.project.hanspoon.common.entity.BaseTimeEntity;
import com.project.hanspoon.common.payment.entity.Payment;
import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.oneday.clazz.entity.ClassSession;
import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "class_reservation",
        indexes = {
                @Index(name = "idx_reservation_session", columnList = "session_id"),
                @Index(name = "idx_reservation_user", columnList = "member_id"),
                @Index(name = "idx_reservation_status", columnList = "status")
        })
public class ClassReservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", foreignKey = @ForeignKey(name = "fk_reservation_session"))
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User user;

    // Legacy DB compatibility: some schemas still keep NOT NULL user_id.
    @Column(name = "user_id", nullable = false)
    private Long legacyUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "hold_expired_at", nullable = false)
    private LocalDateTime holdExpiredAt;

    private LocalDateTime paidAt;
    private LocalDateTime canceledAt;
    private LocalDateTime completedAt;

    // 결제 엔티티 연결(PortOneService 하위호환)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Builder
    private ClassReservation(ClassSession session, User user,
                             ReservationStatus status, LocalDateTime holdExpiredAt) {
        this.session = session;
        this.user = user;
        this.legacyUserId = (user != null ? user.getUserId() : null);
        this.status = status;
        this.holdExpiredAt = holdExpiredAt;
    }

    @PrePersist
    @PreUpdate
    private void syncLegacyUserId() {
        if (this.user != null) {
            this.legacyUserId = this.user.getUserId();
        }
    }

    public boolean isExpired(LocalDateTime now) {
        return holdExpiredAt.isBefore(now);
    }

    public void markPaid(LocalDateTime now) {
        this.status = ReservationStatus.PAID;
        this.paidAt = now;
    }

    public void markCanceled(LocalDateTime now) {
        this.status = ReservationStatus.CANCELED;
        this.canceledAt = now;
    }

    public void markExpired(LocalDateTime now) {
        this.status = ReservationStatus.EXPIRED;
    }

    public void markCompleted(LocalDateTime now) {
        this.status = ReservationStatus.COMPLETED;
        this.completedAt = now;
    }

    // 기존 서비스 코드에서 사용하는 하위호환 세터
    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}