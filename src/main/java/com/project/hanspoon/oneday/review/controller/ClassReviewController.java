package com.project.hanspoon.oneday.review.controller;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.common.response.ApiResponse;
import com.project.hanspoon.common.security.CustomUserDetails;
import com.project.hanspoon.oneday.review.dto.ClassReviewAnswerRequest;
import com.project.hanspoon.oneday.review.dto.ClassReviewCreateRequest;
import com.project.hanspoon.oneday.review.dto.ClassReviewResponse;
import com.project.hanspoon.oneday.review.service.ClassReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oneday/reviews")
public class ClassReviewController {

    private final ClassReviewService reviewService;

    @PostMapping
    public ApiResponse<ClassReviewResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ClassReviewCreateRequest req
    ) {
        Long userId = resolveUserId(userDetails);
        return ApiResponse.ok("리뷰가 등록되었습니다.", reviewService.create(userId, req));
    }

    // 리뷰 답글은 관리자 또는 리뷰 작성자만 달 수 있습니다.
    @PostMapping("/{reviewId}/answer")
    public ApiResponse<ClassReviewResponse> answerByAdmin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody ClassReviewAnswerRequest req
    ) {
        Long userId = resolveUserId(userDetails);
        boolean admin = isAdmin(userDetails);
        return ApiResponse.ok("리뷰 답글이 등록되었습니다.", reviewService.answerByAdmin(userId, admin, reviewId, req));
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        Long userId = resolveUserId(userDetails);
        reviewService.delete(userId, reviewId);
        return ApiResponse.ok("리뷰가 삭제되었습니다. (소프트 삭제)", null);
    }

    @GetMapping("/classes/{classId}")
    public ApiResponse<List<ClassReviewResponse>> listByClass(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long classId
    ) {
        boolean admin = isAdmin(userDetails);
        return ApiResponse.ok(reviewService.listByClass(classId, admin));
    }

    private Long resolveUserId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new BusinessException("로그인 정보가 필요합니다.");
        }
        return userDetails.getUserId();
    }

    private boolean isAdmin(CustomUserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
