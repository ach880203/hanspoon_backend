package com.project.hanspoon.common.user.controller;

import com.project.hanspoon.common.dto.ApiResponse;
import com.project.hanspoon.common.security.CustomUserDetails;
import com.project.hanspoon.common.security.jwt.JwtTokenProvider;
import com.project.hanspoon.common.user.dto.LoginRequest;
import com.project.hanspoon.common.user.dto.LoginResponse;
import com.project.hanspoon.common.user.dto.UserRegisterDto;
import com.project.hanspoon.common.user.dto.FindIdRequest;
import com.project.hanspoon.common.user.dto.FindPasswordRequest;
import com.project.hanspoon.common.user.entity.User;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Login endpoint.
     *
     * Important:
     * - Token authority claims are generated from the authenticated principal.
     * - Response role is normalized to a single token like ROLE_ADMIN.
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

            userService.updateLastLogin(user.getUserId());

            LoginResponse response = LoginResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .userName(user.getUserName())
                    .role(resolveRole(user, userDetails))
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Login success", response));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid email or password."));
        } catch (LockedException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Account is locked."));
        } catch (DisabledException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Account is disabled."));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Login failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Server error occurred."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody UserRegisterDto dto) {
        try {
            userService.register(dto);
            return ResponseEntity.ok(ApiResponse.success("Register success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean available = !userService.isEmailExists(email);
        String message = available ? "Available email." : "Email already exists.";
        return ResponseEntity.ok(ApiResponse.success(message, available));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required."));
        }

        User user = userDetails.getUser();
        LoginResponse response = LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .role(resolveRole(user, userDetails))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<ApiResponse<LoginResponse>> oauth2Success(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("OAuth2 auth failed."));
        }

        User user = userDetails.getUser();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtTokenProvider.createToken(authentication);

        LoginResponse response = LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .role(resolveRole(user, userDetails))
                .build();

        return ResponseEntity.ok(ApiResponse.success("OAuth2 login success", response));
    }

    @GetMapping("/oauth2/failure")
    public ResponseEntity<ApiResponse<Void>> oauth2Failure() {
        return ResponseEntity.badRequest().body(ApiResponse.error("OAuth2 login failed."));
    }

    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<String>> findEmail(@RequestBody FindIdRequest request) {
        try {
            String email = userService.findEmail(request.getUserName(), request.getPhone());
            return ResponseEntity.ok(ApiResponse.success("Find email success", email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody FindPasswordRequest request) {
        try {
            String tempPassword = userService.resetPassword(
                    request.getEmail(), request.getUserName(), request.getPhone());
            return ResponseEntity.ok(ApiResponse.success("Reset password success", tempPassword));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Normalize role for client-side authorization checks.
     *
     * Priority:
     * 1) user.role from DB
     * 2) first granted authority
     * 3) ROLE_USER fallback
     */
    private String resolveRole(User user, CustomUserDetails userDetails) {
        if (user != null && user.getRole() != null && !user.getRole().isBlank()) {
            return user.getRole().trim();
        }

        if (userDetails != null && userDetails.getAuthorities() != null) {
            return userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a != null && !a.isBlank())
                    .findFirst()
                    .orElse("ROLE_USER");
        }

        return "ROLE_USER";
    }
}
