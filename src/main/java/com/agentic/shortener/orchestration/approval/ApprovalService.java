package com.agentic.shortener.orchestration.approval;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ApprovalService {

    private final Map<String, ApprovalRequest> approvals =
            new ConcurrentHashMap<>();

    public ApprovalRequest requestApproval(
            String workflowId,
            String reason
    ) {
        ApprovalRequest request =
                new ApprovalRequest(workflowId, reason);

        approvals.put(workflowId, request);
        return request;
    }

    public ApprovalRequest getApproval(String workflowId) {
        ApprovalRequest request = approvals.get(workflowId);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Approval request not found: " + workflowId
            );
        }

        return request;
    }

    public ApprovalRequest approve(String workflowId) {
        ApprovalRequest request = getApproval(workflowId);
        request.approve();
        return request;
    }

    public ApprovalRequest reject(String workflowId) {
        ApprovalRequest request = getApproval(workflowId);
        request.reject();
        return request;
    }
}