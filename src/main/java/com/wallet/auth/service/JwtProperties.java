package com.wallet.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        long accessExpiryMs,
        long refreshExpiryMs
) {
}

