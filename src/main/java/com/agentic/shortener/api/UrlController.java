package com.agentic.shortener.api;

import com.agentic.shortener.dto.CreateUrlRequest;
import com.agentic.shortener.dto.CreateUrlResponse;
import com.agentic.shortener.dto.UrlAnalyticsResponse;
import com.agentic.shortener.service.UrlShorteningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlShorteningService urlShorteningService;

    public UrlController(UrlShorteningService urlShorteningService) {
        this.urlShorteningService = urlShorteningService;
    }

    @PostMapping
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request
    ) {
        CreateUrlResponse response =
                urlShorteningService.createShortUrl(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode
    ) {
        String originalUrl =
                urlShorteningService.resolveOriginalUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(
            @PathVariable String shortCode
    ) {
        UrlAnalyticsResponse response =
                urlShorteningService.getAnalytics(shortCode);

        return ResponseEntity.ok(response);
    }
}