package com.project.hanspoon.oneday.clazz.controller;

import com.project.hanspoon.common.response.ApiResponse;
import com.project.hanspoon.common.security.CustomUserDetails;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

    /**
     * 원데이 클래스 등록 API입니다.
     * 초보자 참고:
     * - 클래스 기본정보 + 세션 목록을 한 번에 저장합니다.
     * - 현재 정책은 관리자만 등록할 수 있도록 제한했습니다.
     */
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
        return userDetails == null ? null : userDetails.getUserId();
    }

    private boolean isAdmin(CustomUserDetails userDetails) {
        if (userDetails == null) return false;
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
