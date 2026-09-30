package com.agentic.shortener.service;

import com.agentic.shortener.domain.UrlMapping;
import com.agentic.shortener.dto.CreateUrlRequest;
import com.agentic.shortener.dto.CreateUrlResponse;
import com.agentic.shortener.dto.UrlAnalyticsResponse;
import com.agentic.shortener.exception.UrlExpiredException;
import com.agentic.shortener.exception.UrlNotFoundException;
import com.agentic.shortener.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class UrlShorteningService {

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int SHORT_CODE_LENGTH = 7;
    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final String BASE_URL =
            "http://localhost:8080/api/v1/urls/";

    private final UrlMappingRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public UrlShorteningService(UrlMappingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        validateExpiration(request.expiresAt());

        String shortCode = generateUniqueShortCode();

        UrlMapping mapping = new UrlMapping(
                request.originalUrl(),
                shortCode,
                Instant.now(),
                request.expiresAt()
        );

        UrlMapping saved = repository.save(mapping);

        return new CreateUrlResponse(
                saved.getId(),
                saved.getOriginalUrl(),
                saved.getShortCode(),
                BASE_URL + saved.getShortCode(),
                saved.getCreatedAt(),
                saved.getExpiresAt()
        );
    }

    @Transactional
    public String resolveOriginalUrl(String shortCode) {
        UrlMapping mapping = findValidMapping(shortCode);

        mapping.recordAccess();
        repository.save(mapping);

        return mapping.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL not found: " + shortCode
                ));

        return new UrlAnalyticsResponse(
                mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getClickCount(),
                mapping.getCreatedAt(),
                mapping.getLastAccessedAt(),
                mapping.getExpiresAt(),
                mapping.isActive()
        );
    }

    private UrlMapping findValidMapping(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(
                        "Short URL not found: " + shortCode
                ));

        if (!mapping.isActive()) {
            throw new IllegalStateException(
                    "Short URL is inactive: " + shortCode
            );
        }

        if (mapping.getExpiresAt() != null
                && mapping.getExpiresAt().isBefore(Instant.now())) {
            throw new UrlExpiredException(
                    "Short URL has expired: " + shortCode
            );
        }

        return mapping;
    }

    private void validateExpiration(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Expiration time must be in the future"
            );
        }
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0;
             attempt < MAX_GENERATION_ATTEMPTS;
             attempt++) {

            String shortCode = generateShortCode();

            if (!repository.existsByShortCode(shortCode)) {
                return shortCode;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique short code"
        );
    }

    private String generateShortCode() {
        StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }
}