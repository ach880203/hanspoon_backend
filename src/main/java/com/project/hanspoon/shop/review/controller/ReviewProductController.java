package com.project.hanspoon.shop.review.controller;

import com.project.hanspoon.common.security.CustomUserDetails;
import com.project.hanspoon.shop.review.dto.ReviewCreateRequestDto;
import com.project.hanspoon.shop.review.dto.ReviewResponseDto;
import com.project.hanspoon.shop.review.dto.ReviewUpdateRequestDto;
import com.project.hanspoon.shop.review.service.ReviewProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewProductController {

    private final ReviewProductService reviewService;

    // ✅ 상품별 후기 목록 (로그인 없어도 조회 가능하게)
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<Page<ReviewResponseDto>> listByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.listByProduct(productId, page, size));
    }

    // ✅ 내 후기 목록
    @GetMapping("/reviews/me")
    public ResponseEntity<Page<ReviewResponseDto>> myReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = requireUserId(userDetails);
        return ResponseEntity.ok(reviewService.listMyReviews(userId, page, size));
    }

    // ✅ 후기 등록(내 계정)
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ReviewResponseDto> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @RequestBody @Valid ReviewCreateRequestDto req
    ) {
        Long userId = requireUserId(userDetails);
        return ResponseEntity.ok(reviewService.create(userId, productId, req));
    }

    // ✅ 후기 수정(내 후기만)
    @PatchMapping("/reviews/{revId}")
    public ResponseEntity<ReviewResponseDto> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long revId,
            @RequestBody @Valid ReviewUpdateRequestDto req
    ) {
        Long userId = requireUserId(userDetails);
        return ResponseEntity.ok(reviewService.update(userId, revId, req));
    }

    // ✅ 후기 삭제(내 후기만)
    @DeleteMapping("/reviews/{revId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long revId
    ) {
        Long userId = requireUserId(userDetails);
        reviewService.delete(userId, revId);
        return ResponseEntity.noContent().build();
    }

    private Long requireUserId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userDetails.getUser().getUserId();
    }
}
