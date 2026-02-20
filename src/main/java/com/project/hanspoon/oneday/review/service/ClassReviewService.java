package com.project.hanspoon.oneday.review.service;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.common.user.repository.UserRepository;
import com.project.hanspoon.oneday.reservation.domain.ReservationStatus;
import com.project.hanspoon.oneday.reservation.repository.ClassReservationRepository;
import com.project.hanspoon.oneday.review.dto.ClassReviewAnswerRequest;
import com.project.hanspoon.oneday.review.dto.ClassReviewCreateRequest;
import com.project.hanspoon.oneday.review.dto.ClassReviewResponse;
import com.project.hanspoon.oneday.review.entity.ClassReview;
import com.project.hanspoon.oneday.review.repository.ClassReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassReviewService {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final ClassReservationRepository reservationRepository;
    private final ClassReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ClassReviewResponse create(Long userId, ClassReviewCreateRequest req) {
        validateCreateInput(userId, req);

        if (reviewRepository.existsByReservationIdAndDelFlagFalse(req.reservationId())) {
            throw new BusinessException("이미 리뷰가 작성된 예약입니다.");
        }

        var reservation = reservationRepository.findById(req.reservationId())
                .orElseThrow(() -> new BusinessException("예약을 찾을 수 없습니다."));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new BusinessException("본인 예약만 리뷰를 작성할 수 있습니다.");
        }
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new BusinessException("수강 완료(COMPLETED)된 예약만 리뷰를 작성할 수 있습니다.");
        }

        var classProduct = reservation.getSession().getClassProduct();

        ClassReview saved = reviewRepository.save(
                ClassReview.of(classProduct, userId, req.reservationId(), req.rating(), req.content().trim())
        );

        String reviewerName = reservation.getUser().getUserName();
        return new ClassReviewResponse(
                saved.getId(),
                classProduct.getId(),
                saved.getUserId(),
                saved.getReservationId(),
                reviewerName,
                saved.getRating(),
                saved.getContent(),
                saved.getAnswerContent(),
                saved.getAnsweredByUserId(),
                "",
                saved.getAnsweredAt(),
                false,
                saved.getCreatedAt()
        );
    }

    public ClassReviewResponse answerByAdmin(Long actorUserId, boolean isAdmin, Long reviewId, ClassReviewAnswerRequest req) {
        // 답글 권한은 관리자만 허용합니다.
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("로그인 정보가 올바르지 않습니다.");
        }
        if (!isAdmin) {
            throw new BusinessException("리뷰 답글은 관리자만 등록할 수 있습니다.");
        }
        if (reviewId == null || reviewId <= 0) {
            throw new BusinessException("reviewId가 올바르지 않습니다.");
        }
        if (req == null || req.answerContent() == null || req.answerContent().isBlank()) {
            throw new BusinessException("answerContent는 필수입니다.");
        }

        String answer = req.answerContent().trim();
        if (answer.length() > 2000) {
            throw new BusinessException("answerContent는 최대 2000자입니다.");
        }

        ClassReview review = reviewRepository.findByIdAndDelFlagFalse(reviewId)
                .orElseThrow(() -> new BusinessException("리뷰를 찾을 수 없습니다."));

        review.answerByAdmin(answer, actorUserId, LocalDateTime.now(KST_ZONE));

        String reviewerName = userRepository.findById(review.getUserId())
                .map(u -> u.getUserName())
                .orElse("이름 없는 사용자");
        String answeredByName = userRepository.findById(actorUserId)
                .map(u -> u.getUserName())
                .orElse("관리자");

        return new ClassReviewResponse(
                review.getId(),
                review.getClassProduct().getId(),
                review.getUserId(),
                review.getReservationId(),
                reviewerName,
                review.getRating(),
                review.getContent(),
                review.getAnswerContent(),
                review.getAnsweredByUserId(),
                answeredByName,
                review.getAnsweredAt(),
                true,
                review.getCreatedAt()
        );
    }

    public void delete(Long userId, Long reviewId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("로그인 정보가 올바르지 않습니다.");
        }
        if (reviewId == null || reviewId <= 0) {
            throw new BusinessException("리뷰 ID가 올바르지 않습니다.");
        }

        ClassReview review = reviewRepository.findByIdAndDelFlagFalse(reviewId)
                .orElseThrow(() -> new BusinessException("리뷰를 찾을 수 없습니다."));

        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        review.markDeleted(LocalDateTime.now(KST_ZONE));
    }

    @Transactional(readOnly = true)
    public List<ClassReviewResponse> listByClass(Long classId, boolean isAdmin) {
        List<ClassReview> reviews = reviewRepository.findAllByClassProduct_IdAndDelFlagFalseOrderByCreatedAtDesc(classId);
        Map<Long, String> nameByUserId = userRepository.findAllById(
                reviews.stream()
                        .flatMap(rv -> Stream.of(rv.getUserId(), rv.getAnsweredByUserId()))
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(u -> u.getUserId(), u -> u.getUserName(), (a, b) -> a));

        return reviews.stream()
                .map(rv -> new ClassReviewResponse(
                        rv.getId(),
                        rv.getClassProduct().getId(),
                        rv.getUserId(),
                        rv.getReservationId(),
                        nameByUserId.getOrDefault(rv.getUserId(), "이름 없는 사용자"),
                        rv.getRating(),
                        rv.getContent(),
                        rv.getAnswerContent(),
                        rv.getAnsweredByUserId(),
                        nameByUserId.getOrDefault(rv.getAnsweredByUserId(), ""),
                        rv.getAnsweredAt(),
                        isAdmin,
                        rv.getCreatedAt()
                ))
                .toList();
    }

    private void validateCreateInput(Long userId, ClassReviewCreateRequest req) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("로그인 정보가 올바르지 않습니다.");
        }
        if (req == null) {
            throw new BusinessException("리뷰 요청 값이 없습니다.");
        }
        if (req.reservationId() == null || req.reservationId() <= 0) {
            throw new BusinessException("reservationId가 올바르지 않습니다.");
        }
        if (req.rating() < 1 || req.rating() > 5) {
            throw new BusinessException("rating은 1~5여야 합니다.");
        }
        if (req.content() == null || req.content().isBlank()) {
            throw new BusinessException("content는 필수입니다.");
        }
    }
}
