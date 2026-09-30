package com.agentic.shortener.dto;

import java.time.Instant;

public record CreateUrlResponse(
        Long id,
        String originalUrl,
        String shortCode,
        String shortUrl,
        Instant createdAt,
        Instant expiresAt
) {
}