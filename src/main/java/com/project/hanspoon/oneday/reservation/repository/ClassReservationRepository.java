package com.project.hanspoon.oneday.reservation.repository;

import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;
import com.project.hanspoon.oneday.reservation.entity.ClassReservation;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassReservationRepository extends JpaRepository<ClassReservation, Long> {

        boolean existsBySession_IdAndUser_UserIdAndStatusIn(
                        Long sessionId,
                        Long userId, Collection<ReservationStatus> statuses);

        List<ClassReservation> findBySession_IdAndUser_UserId(Long sessionId, Long userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select r from ClassReservation r where r.id = :id")
        Optional<ClassReservation> findByIdForUpdate(@Param("id") Long id);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select r from ClassReservation r where r.id = :id and r.user.userId = :userId")
        Optional<ClassReservation> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

        @Query("""
                            select r
                            from ClassReservation r
                            where r.status = :status
                                and r.holdExpiredAt is not null
                              and r.holdExpiredAt < :now
                        """)
        List<ClassReservation> findExpiredHolds(
                        @Param("status") ReservationStatus status,
                        @Param("now") LocalDateTime now);

        @Query("select r from ClassReservation r where r.user.userId = :userId order by r.createdAt desc")
        List<ClassReservation> findByUserId(@Param("userId") Long userId);

        List<ClassReservation> findAllByUser_UserId(Long userId, Sort sort);

        List<ClassReservation> findAllByUser_UserIdAndStatus(Long userId, ReservationStatus status, Sort sort);

        Optional<ClassReservation> findByIdAndUser_UserId(Long reservationId, Long userId);

        @Query("""
                            select r
                            from ClassReservation r
                            join fetch r.session s
                            join fetch s.classProduct p
                            where r.status = :status
                              and s.startAt < :now
                        """)
        List<ClassReservation> findPaidToComplete(
                        @Param("status") ReservationStatus status,
                        @Param("now") LocalDateTime now);

        @Query("select count(r) from ClassReservation r where r.status = :status")
        long countByStatus(@Param("status") ReservationStatus status);

        List<ClassReservation> findByStatus(ReservationStatus status);

        @Query("select count(r) from ClassReservation r join r.session s where s.startAt between :start and :end and r.status in :statuses")
        long countBySessionStartAtBetweenAndStatusIn(
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end,
                        @Param("statuses") Collection<ReservationStatus> statuses);

        long countByCreatedAtBetweenAndStatusIn(LocalDateTime start, LocalDateTime end,
                        Collection<ReservationStatus> statuses);
}
