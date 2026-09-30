package com.agentic.shortener.orchestration.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/api/v1/workflows/metrics")
    public WorkflowMetrics metrics() {
        return metricsService.getMetrics();
    }
}