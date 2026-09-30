package com.agentic.shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record CreateUrlRequest(

        @NotBlank(message = "URL is required")
        @Pattern(
                regexp = "^https?://.+$",
                message = "URL must start with http:// or https://"
        )
        String originalUrl,

        Instant expiresAt
) {
}