package com.project.hanspoon.oneday.review.service;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.common.user.entity.User;
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
import java.util.stream.Collectors;

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

        // 동일 예약에 대한 리뷰는 한 건만 허용합니다.
        if (reviewRepository.existsByReservationIdAndDelFlagFalse(req.reservationId())) {
            throw new BusinessException("이미 리뷰가 작성된 예약입니다.");
        }

        var reservation = reservationRepository.findById(req.reservationId())
                .orElseThrow(() -> new BusinessException("예약을 찾을 수 없습니다."));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new BusinessException("본인 예약만 리뷰를 작성할 수 있습니다.");
        }
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new BusinessException("수강 완료 상태인 예약만 리뷰를 작성할 수 있습니다.");
        }

        var classProduct = reservation.getSession().getClassProduct();
        ClassReview saved = reviewRepository.save(
                ClassReview.of(classProduct, userId, req.reservationId(), req.rating(), req.content().trim())
        );

        String reviewerName = reservation.getUser().getUserName();
        return toResponse(saved, reviewerName, null, false);
    }

    // 리뷰 답글은 관리자만 등록할 수 있습니다.
    public ClassReviewResponse answer(Long actorUserId, boolean isAdmin, Long reviewId, ClassReviewAnswerRequest req) {
        if (actorUserId == null || actorUserId <= 0) {
            throw new BusinessException("로그인 정보가 올바르지 않습니다.");
        }
        if (!isAdmin) {
            throw new BusinessException("리뷰 답글은 관리자만 작성할 수 있습니다.");
        }
        if (reviewId == null || reviewId <= 0) {
            throw new BusinessException("리뷰 ID가 올바르지 않습니다.");
        }
        if (req == null || req.answerContent() == null || req.answerContent().isBlank()) {
            throw new BusinessException("답글 내용을 입력해 주세요.");
        }

        ClassReview review = reviewRepository.findByIdAndDelFlagFalse(reviewId)
                .orElseThrow(() -> new BusinessException("리뷰를 찾을 수 없습니다."));

        String answer = req.answerContent().trim();
        review.answer(answer, actorUserId, LocalDateTime.now(KST_ZONE));

        String reviewerName = userRepository.findById(review.getUserId())
                .map(User::getUserName)
                .orElse("이름 없음");
        String answeredByName = userRepository.findById(actorUserId)
                .map(User::getUserName)
                .orElse("관리자");

        return toResponse(review, reviewerName, answeredByName, true);
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
    public List<ClassReviewResponse> listByClass(Long classId, Long viewerUserId, boolean viewerIsAdmin) {
        List<ClassReview> reviews = reviewRepository.findAllByClassProduct_IdAndDelFlagFalseOrderByCreatedAtDesc(classId);

        // 작성자명/답글작성자명을 한 번에 가져오기 위해 사용자 ID를 모아 조회합니다.
        List<Long> userIds = reviews.stream()
                .flatMap(rv -> java.util.stream.Stream.of(rv.getUserId(), rv.getAnsweredByUserId()))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        Map<Long, String> nameByUserId = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getUserName, (a, b) -> a));

        return reviews.stream()
                .map(rv -> {
                    String reviewerName = nameByUserId.getOrDefault(rv.getUserId(), "이름 없음");
                    String answeredByName = rv.getAnsweredByUserId() == null
                            ? null
                            : nameByUserId.getOrDefault(rv.getAnsweredByUserId(), "관리자");
                    boolean canAnswer = viewerIsAdmin;
                    return toResponse(rv, reviewerName, answeredByName, canAnswer);
                })
                .toList();
    }

    private ClassReviewResponse toResponse(
            ClassReview rv,
            String reviewerName,
            String answeredByName,
            boolean canAnswer
    ) {
        return new ClassReviewResponse(
                rv.getId(),
                rv.getClassProduct().getId(),
                rv.getUserId(),
                rv.getReservationId(),
                reviewerName,
                rv.getRating(),
                rv.getContent(),
                rv.getCreatedAt(),
                rv.getAnswerContent(),
                rv.getAnsweredByUserId(),
                answeredByName,
                rv.getAnsweredAt(),
                canAnswer
        );
    }

    private void validateCreateInput(Long userId, ClassReviewCreateRequest req) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("로그인 정보가 올바르지 않습니다.");
        }
        if (req == null) {
            throw new BusinessException("리뷰 요청 값이 없습니다.");
        }
        if (req.reservationId() == null || req.reservationId() <= 0) {
            throw new BusinessException("예약 ID가 올바르지 않습니다.");
        }
        if (req.rating() < 1 || req.rating() > 5) {
            throw new BusinessException("평점은 1~5여야 합니다.");
        }
        if (req.content() == null || req.content().isBlank()) {
            throw new BusinessException("리뷰 내용은 필수입니다.");
        }
    }
}
