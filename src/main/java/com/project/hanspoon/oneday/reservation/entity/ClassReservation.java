package com.project.hanspoon.oneday.reservation.entity;

import com.project.hanspoon.common.entity.BaseTimeEntity;
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
@Table(name = "class_reservation", indexes = {
        @Index(name = "idx_reservation_session", columnList = "session_id"),
        @Index(name = "idx_reservation_user", columnList = "member_id"),
        @Index(name = "idx_reservation_status", columnList = "status")
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "createdat")),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updatedat"))
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
    @Column(name = "user_id", nullable = true)
    private Long legacyUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "hold_expired_at", nullable = false)
    private LocalDateTime holdExpiredAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_id")
    private com.project.hanspoon.common.payment.entity.Payment payment; // 결제 정보 연결 (환불용)

    public void setPayment(com.project.hanspoon.common.payment.entity.Payment payment) {
        this.payment = payment;
    }

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
        if (holdExpiredAt == null)
            return true; // 만료 시간이 없으면 만료된 것으로 간주 (혹은 필요에 따라 정책 결정)
        return holdExpiredAt.isBefore(now);
    }

    public void markPaid(LocalDateTime now) {
        this.status = ReservationStatus.PAID;
        this.paidAt = now;
    }

    public void markCancelRequested(LocalDateTime now, String reason) {
        this.status = ReservationStatus.CANCEL_REQUESTED;
        this.cancelRequestedAt = now;
        this.cancelReason = reason;
    }

    public void markCanceled(LocalDateTime now) {
        this.status = ReservationStatus.CANCELED;
        this.canceledAt = now;
    }

    public void revertToPaid() {
        this.status = ReservationStatus.PAID;
        this.cancelRequestedAt = null;
        this.cancelReason = null;
    }

    public void markExpired(LocalDateTime now) {
        this.status = ReservationStatus.EXPIRED;
    }

    public void markCompleted(LocalDateTime now) {
        this.status = ReservationStatus.COMPLETED;
        this.completedAt = now;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
