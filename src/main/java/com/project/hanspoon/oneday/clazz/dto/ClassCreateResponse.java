package com.project.hanspoon.oneday.clazz.dto;

import java.util.List;

// 클래스 등록 완료 후 프런트에 돌려줄 응답 DTO입니다.
public record ClassCreateResponse(
        Long classId,
        String title,
        Long instructorId,
        int sessionCount,
        List<Long> sessionIds
) {
}
