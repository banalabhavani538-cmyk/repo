package com.agentic.shortener.orchestration.engine;

import com.agentic.shortener.orchestration.state.WorkflowStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkflowExecution {

    private final String workflowId = UUID.randomUUID().toString();

    private WorkflowStatus status = WorkflowStatus.CREATED;

    private final Instant startedAt = Instant.now();

    private final List<WorkflowTask> tasks = new ArrayList<>();
    private String approvalReason;

    public String getWorkflowId() {
        return workflowId;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public List<WorkflowTask> getTasks() {
        return tasks;
    }

    public void addTask(WorkflowTask task) {
        tasks.add(task);
    }
    public String getApprovalReason() {
        return approvalReason;
    }

    public void setApprovalReason(String approvalReason) {
        this.approvalReason = approvalReason;
    }
}