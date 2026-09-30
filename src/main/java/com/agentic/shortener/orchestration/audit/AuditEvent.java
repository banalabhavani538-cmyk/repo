package com.agentic.shortener.orchestration.audit;

import java.time.Instant;
import java.util.UUID;

public class AuditEvent {

    private final String eventId;
    private final String workflowId;
    private final AuditEventType eventType;
    private final String message;
    private final Instant timestamp;

    public AuditEvent(
            String workflowId,
            AuditEventType eventType,
            String message
    ) {
        this.eventId = UUID.randomUUID().toString();
        this.workflowId = workflowId;
        this.eventType = eventType;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}