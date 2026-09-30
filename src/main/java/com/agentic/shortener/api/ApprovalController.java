package com.agentic.shortener.api;

import com.agentic.shortener.orchestration.approval.ApprovalRequest;
import com.agentic.shortener.orchestration.approval.ApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<ApprovalRequest> getApproval(
            @PathVariable String workflowId
    ) {
        return ResponseEntity.ok(
                approvalService.getApproval(workflowId)
        );
    }

    @PostMapping("/{workflowId}/approve")
    public ResponseEntity<ApprovalRequest> approve(
            @PathVariable String workflowId
    ) {
        return ResponseEntity.ok(
                approvalService.approve(workflowId)
        );
    }

    @PostMapping("/{workflowId}/reject")
    public ResponseEntity<ApprovalRequest> reject(
            @PathVariable String workflowId
    ) {
        return ResponseEntity.ok(
                approvalService.reject(workflowId)
        );
    }
}