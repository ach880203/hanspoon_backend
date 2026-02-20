package com.project.hanspoon.oneday.clazz.controller;

import com.project.hanspoon.common.exception.BusinessException;
import com.project.hanspoon.common.response.ApiResponse;
import com.project.hanspoon.oneday.clazz.domain.Level;
import com.project.hanspoon.oneday.clazz.domain.RecipeCategory;
import com.project.hanspoon.oneday.clazz.domain.RunType;
import com.project.hanspoon.oneday.clazz.domain.SessionSlot;
import com.project.hanspoon.oneday.clazz.dto.ClassCreateRequest;
import com.project.hanspoon.oneday.clazz.dto.ClassCreateResponse;
import com.project.hanspoon.oneday.clazz.dto.ClassDetailResponse;
import com.project.hanspoon.oneday.clazz.dto.ClassListItemResponse;
import com.project.hanspoon.oneday.clazz.dto.SessionResponse;
import com.project.hanspoon.oneday.clazz.service.ClassCommandService;
import com.project.hanspoon.oneday.clazz.service.ClassQueryService;
import com.project.hanspoon.common.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oneday/classes")
public class ClassController {

    private final ClassQueryService classQueryService;
    private final ClassCommandService classCommandService;

    @PostMapping
    public ApiResponse<ClassCreateResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ClassCreateRequest req
    ) {
        Long userId = resolveUserId(userDetails);
        boolean admin = isAdmin(userDetails);
        return ApiResponse.ok("원데이 클래스가 등록되었습니다.", classCommandService.createClass(userId, admin, req));
    }

    @GetMapping
    public ApiResponse<Page<ClassListItemResponse>> list(
            @RequestParam(required = false)Level level,
            @RequestParam(required = false)RunType runType,
            @RequestParam(required = false)RecipeCategory category,
            @RequestParam(required = false) Long instructorId,
            Pageable pageable
    ) {
        // pageable 예시: /classes?page=0&size=10&sort=createdAt,desc
        return ApiResponse.ok(classQueryService.searchClasses(level, runType, category, instructorId, pageable));
    }

    @GetMapping("/{classId}")
    public ApiResponse<ClassDetailResponse> detail(@PathVariable Long classId) {
        return ApiResponse.ok(classQueryService.getClassDetail(classId));
    }

    @GetMapping("/{classId}/sessions")
    public ApiResponse<List<SessionResponse>> sessions(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date,
            @RequestParam(required = false)SessionSlot slot
    ) {
        return ApiResponse.ok(classQueryService.getSessions(classId, date, slot));
    }

    private Long resolveUserId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new BusinessException("로그인 정보가 필요합니다.");
        }
        return userDetails.getUserId();
    }

    private boolean isAdmin(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getAuthorities() == null) return false;
        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
}
