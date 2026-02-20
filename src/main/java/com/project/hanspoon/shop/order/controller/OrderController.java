package com.project.hanspoon.shop.order.controller;

import com.project.hanspoon.common.security.CustomUserDetails;
import com.project.hanspoon.shop.order.dto.*;
import com.project.hanspoon.shop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    // =========================
    // ✅ me 기반 (프론트 유저용)
    // =========================

    // 주문 생성: 내 장바구니 -> 주문
    @PostMapping("/me")
    public ResponseEntity<OrderResponseDto> createMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid OrderCreateMyRequestDto req
    ) {
        Long userId = requireUserId(userDetails);
        OrderResponseDto created = orderService.createOrderFromMyCart(userId, req);
        return ResponseEntity.created(URI.create("/api/orders/me/" + created.getOrderId())).body(created);
    }

    // 내 주문 목록 (필터링 추가)
    @GetMapping("/me")
    public ResponseEntity<Page<OrderListItemDto>> listMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(required = false) com.project.hanspoon.shop.constant.OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = requireUserId(userDetails);
        
        java.time.LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        java.time.LocalDateTime end = (endDate != null) ? endDate.atTime(23, 59, 59) : null;
        
        return ResponseEntity.ok(orderService.getMyOrdersWithFilters(userId, start, end, status, page, size));
    }

    // 내 주문 상세
    @GetMapping("/me/{orderId}")
    public ResponseEntity<OrderResponseDto> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
        Long userId = requireUserId(userDetails);
        return ResponseEntity.ok(orderService.getMyOrder(userId, orderId));
    }

    // 내 주문 취소/환불
    @PostMapping("/me/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody(required = false) OrderCancelRequestDto req
    ) {
        Long userId = requireUserId(userDetails);
        String reason = (req == null) ? null : req.getReason();
        return ResponseEntity.ok(orderService.cancelMyOrder(userId, orderId, reason));
    }

    // 내 주문 결제
    @PostMapping("/me/{orderId}/pay")
    public ResponseEntity<OrderResponseDto> payMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody(required = false) OrderPayRequestDto req
    ) {
        Long userId = requireUserId(userDetails);
        String method = (req == null) ? null : req.getPayMethod();
        return ResponseEntity.ok(orderService.payMyOrder(userId, orderId, method));
    }

    // =========================
    // ✅ 기존 엔드포인트 유지(관리/테스트용)
    // - 너 기존 프론트 버튼(Ship/Deliver) 때문에 남겨둠
    // =========================

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<OrderResponseDto> ship(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.ship(orderId));
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<OrderResponseDto> deliver(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.deliver(orderId));
    }

    // =========================
    // 공통
    // =========================
    private Long requireUserId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userDetails.getUser().getUserId();
    }
}
