package com.agentic.shortener.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "url_mappings")
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    protected UrlMapping() {
    }

    public UrlMapping(
            String originalUrl,
            String shortCode,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }
    @Column(nullable = false)
    private long clickCount = 0;

    private Instant lastAccessedAt;
    public long getClickCount() {
        return clickCount;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
    public void recordAccess() {
        this.clickCount++;
        this.lastAccessedAt = Instant.now();
    }
}