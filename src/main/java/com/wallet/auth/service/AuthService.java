package com.wallet.auth.service;

import com.wallet.auth.dto.LoginRequest;
import com.wallet.auth.dto.RefreshTokenRequest;
import com.wallet.auth.dto.RegisterRequest;
import com.wallet.auth.dto.TokenResponse;
import com.wallet.auth.model.RefreshToken;
import com.wallet.auth.model.User;
import com.wallet.auth.repository.RefreshTokenRepository;
import com.wallet.auth.repository.UserRepository;
import com.wallet.common.exception.BadRequestException;
import jakarta.transaction.Transactional;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        userRepository.save(user);

        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.password())
        );

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found"));

        refreshTokenRepository.deleteAllByUserId(user.getId());
        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken =
                refreshTokenRepository.findByToken(request.refreshToken())
                        .orElseThrow(() -> new BadCredentialsException(
                                "Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new BadCredentialsException("Refresh token has expired");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!jwtService.isTokenValid(request.refreshToken(), user,
                "refresh")) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        refreshTokenRepository.delete(storedToken);
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(
                Instant.now().plusMillis(jwtProperties.refreshExpiryMs()));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue, "Bearer",
                jwtService.getAccessExpiryMs());
    }
}

