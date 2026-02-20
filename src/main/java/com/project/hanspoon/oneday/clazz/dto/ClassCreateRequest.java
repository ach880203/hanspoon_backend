package com.project.hanspoon.oneday.clazz.dto;

import com.project.hanspoon.oneday.clazz.domain.Level;
import com.project.hanspoon.oneday.clazz.domain.RecipeCategory;
import com.project.hanspoon.oneday.clazz.domain.RunType;

import java.util.List;

// 원데이 클래스 등록 요청 DTO입니다.
// 프런트에서 클래스 기본 정보 + 세션 배열을 함께 보냅니다.
public record ClassCreateRequest(
        String title,
        String description,
        Level level,
        RunType runType,
        RecipeCategory category,
        Long instructorId,
        List<ClassSessionCreateRequest> sessions
) {
}
