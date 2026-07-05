package com.society.app.member;

import com.society.app.common.ApiResponse;
import com.society.app.member.dto.AuthResponse;
import com.society.app.member.dto.ChangePasswordRequest;
import com.society.app.member.dto.LoginRequest;
import com.society.app.member.dto.LogoutRequest;
import com.society.app.member.dto.MemberDto;
import com.society.app.member.dto.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints — Phase 2 surface area:
 * <ul>
 *   <li>POST /auth/login           (public)</li>
 *   <li>POST /auth/refresh         (public)</li>
 *   <li>POST /auth/logout          (auth)</li>
 *   <li>GET  /auth/me              (auth)</li>
 *   <li>POST /auth/change-password (auth)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse data = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest req) {
        AuthResponse data = authService.refresh(req);
        return ResponseEntity.ok(ApiResponse.ok(data, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest req,
                                                    Authentication authentication) {
        authService.logout(authentication.getName(), req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberDto>> me(Authentication authentication) {
        MemberDto data = authService.getMe(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }
}
