package com.project.hanspoon.admin.controller;

import com.project.hanspoon.admin.dto.AdminDashboardSummaryDto;
import com.project.hanspoon.admin.service.AdminDashboardService;
import com.project.hanspoon.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryDto>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardSummary()));
    }
}
