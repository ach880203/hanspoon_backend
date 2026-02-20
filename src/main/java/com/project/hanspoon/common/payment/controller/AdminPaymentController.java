package com.project.hanspoon.common.payment.controller;

import com.project.hanspoon.common.dto.ApiResponse;
import com.project.hanspoon.common.dto.PageResponse;
import com.project.hanspoon.common.payment.dto.PaymentDto;
import com.project.hanspoon.common.payment.entity.Payment;
import com.project.hanspoon.common.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자용 결제 관리 Controller
 */
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    /**
     * 전체 결제 내역 조회 (페이지네이션)
     * GET /api/admin/payments/list
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<PaymentDto>>> getPaymentList(
            @PageableDefault(size = 10, sort = "payDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Payment> payments = paymentService.findAll(pageable);
        Page<PaymentDto> dtoPage = payments.map(PaymentDto::from);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(dtoPage)));
    }

    /**
     * 결제 취소 (환불)
     * POST /api/admin/payments/{payId}/cancel
     */
    @org.springframework.web.bind.annotation.PostMapping("/{payId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(
            @org.springframework.web.bind.annotation.PathVariable Long payId) {
        try {
            paymentService.cancelPayment(payId);
            return ResponseEntity.ok(ApiResponse.success("결제가 취소되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
