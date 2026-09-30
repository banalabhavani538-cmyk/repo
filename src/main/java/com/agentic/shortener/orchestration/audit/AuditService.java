package com.agentic.shortener.orchestration.audit;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuditService {

    private final Map<String, List<AuditEvent>> auditEvents =
            new ConcurrentHashMap<>();

    public AuditEvent record(
            String workflowId,
            AuditEventType eventType,
            String message
    ) {

        AuditEvent auditEvent =
                new AuditEvent(
                        workflowId,
                        eventType,
                        message
                );

        auditEvents
                .computeIfAbsent(
                        workflowId,
                        key -> new CopyOnWriteArrayList<>()
                )
                .add(auditEvent);

        return auditEvent;
    }

    public List<AuditEvent> getEvents(
            String workflowId
    ) {

        return List.copyOf(
                auditEvents.getOrDefault(
                        workflowId,
                        List.of()
                )
        );
    }
}