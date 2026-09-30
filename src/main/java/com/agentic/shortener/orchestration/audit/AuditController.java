package com.agentic.shortener.orchestration.audit;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
public class AuditController {

    private final AuditService auditService;

    public AuditController(
            AuditService auditService
    ) {
        this.auditService = auditService;
    }

    @GetMapping("/{workflowId}/audit")
    public List<AuditEvent> getAuditEvents(
            @PathVariable String workflowId
    ) {
        return auditService.getEvents(workflowId);
    }
}