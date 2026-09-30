package com.agentic.shortener.orchestration.metrics;

public record WorkflowMetrics(
        long totalWorkflows,
        long completedWorkflows,
        long waitingForApproval,
        long rolledBackWorkflows,
        long rejectedWorkflows
) {
}