package com.project.hanspoon.oneday.clazz.dto;

import com.project.hanspoon.oneday.clazz.domain.SessionSlot;

import java.time.LocalDateTime;

// 원데이 클래스 등록 시 함께 생성할 세션(오전/오후) 입력 DTO입니다.
public record ClassSessionCreateRequest(
        LocalDateTime startAt,
        SessionSlot slot,
        Integer capacity,
        Integer price
) {
}
