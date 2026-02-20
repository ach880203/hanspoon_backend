package com.project.hanspoon.common.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        OffsetDateTime timestamp
) {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "정상 처리되었습니다.", null, OffsetDateTime.now(KST));
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, data, message, null, OffsetDateTime.now(KST));
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, "BUSINESS_ERROR", OffsetDateTime.now(KST));
    }

    public static <T> ApiResponse<T> fail(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode, OffsetDateTime.now(KST));
    }

    public static <T> ApiResponse<T> error(String message) {
        return fail(message, "BUSINESS_ERROR");
    }
}
