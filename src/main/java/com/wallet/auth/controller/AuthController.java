package com.wallet.auth.controller;

import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.RefreshTokenRequest;
import com.wallet.auth.dto.RegisterRequest;
import com.wallet.auth.dto.TokenResponse;
import com.wallet.auth.service.AuthService;
import com.wallet.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for account registration, authentication, token refresh, and session termination.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user account and returns an access token plus refresh token for immediate authenticated use.")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully",
                        authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user", description = "Validates the supplied credentials and returns a fresh access token and refresh token pair.")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful",
                authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh an access token", description = "Exchanges a valid refresh token for a new token pair without asking the user to log in again.")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully",
                        authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out a session", description = "Invalidates the supplied refresh token so it can no longer be used to obtain new access tokens.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(
                ApiResponse.success("Logout successful", null));
    }
}

