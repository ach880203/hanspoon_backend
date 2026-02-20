package com.project.hanspoon.common.exception;

import com.project.hanspoon.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        System.err.println("[BusinessException] " + e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage(), "BUSINESS_ERROR"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        System.err.println("[ValidationException] " + message);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(ApiResponse.fail("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
    }
}
