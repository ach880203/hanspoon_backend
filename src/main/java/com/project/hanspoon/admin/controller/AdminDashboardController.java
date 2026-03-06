package com.project.hanspoon.admin.controller;

import com.project.hanspoon.admin.dto.AdminDashboardSummaryDto;
import com.project.hanspoon.admin.service.AdminDashboardService;
import com.project.hanspoon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryDto>> getDashboardSummary() {
        AdminDashboardSummaryDto summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/sales-trend")
    public ResponseEntity<ApiResponse<com.project.hanspoon.admin.dto.SalesTrendDto>> getSalesTrend(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSalesTrend(days)));
    }

    @GetMapping("/sales/statistics")
    public ResponseEntity<ApiResponse<com.project.hanspoon.admin.dto.SalesStatisticsDto>> getSalesStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSalesStatistics()));
    }
}
