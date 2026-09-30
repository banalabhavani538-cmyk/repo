package com.agentic.shortener.dto;

import java.time.Instant;

public record UrlAnalyticsResponse(
        String shortCode,
        String originalUrl,
        long clickCount,
        Instant createdAt,
        Instant lastAccessedAt,
        Instant expiresAt,
        boolean active
) {
}