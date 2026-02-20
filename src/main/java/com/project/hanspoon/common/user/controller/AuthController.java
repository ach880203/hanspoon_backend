package com.project.hanspoon.common.user.controller;

import com.project.hanspoon.common.dto.ApiResponse;
import com.project.hanspoon.common.user.dto.LoginRequest;
import com.project.hanspoon.common.user.dto.LoginResponse;
import com.project.hanspoon.common.user.dto.UserRegisterDto;
import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.common.security.CustomUserDetails;
import com.project.hanspoon.common.security.jwt.JwtTokenProvider;
import com.project.hanspoon.common.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 REST API Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String token = jwtTokenProvider.createToken(authentication);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            // 최근 로그인 일시 업데이트
            userService.updateLastLogin(user.getUserId());

            LoginResponse response = LoginResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .userName(user.getUserName())
                    .role(userDetails.getAuthorities().toString())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 또는 비밀번호가 올바르지 않습니다."));
        } catch (LockedException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("정지된 계정입니다. 관리자에게 문의하세요."));
        } catch (DisabledException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("탈퇴한 회원입니다."));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("로그인에 실패했습니다: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("서버 오류가 발생했습니다."));
        }
    }

    /**
     * 회원가입
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody UserRegisterDto dto) {
        try {
            userService.register(dto);
            return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 이메일 중복 확인
     * GET /api/auth/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean available = !userService.isEmailExists(email);
        String message = available ? "사용 가능한 이메일입니다." : "이미 사용중인 이메일입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }

    /**
     * 현재 사용자 정보
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("로그인이 필요합니다."));
        }

        User user = userDetails.getUser();
        LoginResponse response = LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .role(userDetails.getAuthorities().toString())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * OAuth2 로그인 성공 핸들러
     * GET /api/auth/oauth2/success
     */
    @GetMapping("/oauth2/success")
    public ResponseEntity<ApiResponse<LoginResponse>> oauth2Success(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("OAuth2 인증 실패"));
        }

        User user = userDetails.getUser();

        // JWT 토큰 생성을 위해 Authentication 객체 생성
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
        String token = jwtTokenProvider.createToken(authentication);

        LoginResponse response = LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .role(userDetails.getAuthorities().toString())
                .build();

        return ResponseEntity.ok(ApiResponse.success("소셜 로그인 성공", response));
    }

    /**
     * OAuth2 로그인 실패 핸들러
     * GET /api/auth/oauth2/failure
     */
    @GetMapping("/oauth2/failure")
    public ResponseEntity<ApiResponse<Void>> oauth2Failure() {
        return ResponseEntity.badRequest().body(ApiResponse.error("소셜 로그인에 실패했습니다."));
    }

    /**
     * 아이디 찾기
     * POST /api/auth/find-email
     */
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<String>> findEmail(
            @RequestBody com.project.hanspoon.common.user.dto.FindIdRequest request) {
        try {
            String email = userService.findEmail(request.getUserName(), request.getPhone());
            return ResponseEntity.ok(ApiResponse.success("아이디 찾기 성공", email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 비밀번호 찾기 (재설정)
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody com.project.hanspoon.common.user.dto.FindPasswordRequest request) {
        try {
            String tempPassword = userService.resetPassword(request.getEmail(), request.getUserName(),
                    request.getPhone());
            return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 성공", tempPassword));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
