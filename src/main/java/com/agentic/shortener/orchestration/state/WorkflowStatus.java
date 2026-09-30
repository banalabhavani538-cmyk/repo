package com.agentic.shortener.orchestration.state;

public enum WorkflowStatus {
    CREATED,
    RUNNING,
    WAITING_FOR_APPROVAL,
    APPROVED,
    REJECTED,
    COMPLETED,
    FAILED,
    ROLLED_BACK,
    SAFE_STOPPED
}