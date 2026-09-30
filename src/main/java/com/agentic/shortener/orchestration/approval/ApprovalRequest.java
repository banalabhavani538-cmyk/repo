package com.agentic.shortener.orchestration.approval;

import java.time.Instant;

public class ApprovalRequest {

    private final String workflowId;
    private final String reason;
    private ApprovalDecision decision;
    private final Instant requestedAt;
    private Instant decidedAt;

    public ApprovalRequest(String workflowId, String reason) {
        this.workflowId = workflowId;
        this.reason = reason;
        this.decision = ApprovalDecision.PENDING;
        this.requestedAt = Instant.now();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getReason() {
        return reason;
    }

    public ApprovalDecision getDecision() {
        return decision;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void approve() {
        this.decision = ApprovalDecision.APPROVED;
        this.decidedAt = Instant.now();
    }

    public void reject() {
        this.decision = ApprovalDecision.REJECTED;
        this.decidedAt = Instant.now();
    }
}