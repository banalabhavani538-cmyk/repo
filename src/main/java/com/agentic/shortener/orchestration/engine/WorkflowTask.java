package com.agentic.shortener.orchestration.engine;

import com.agentic.shortener.orchestration.state.TaskStatus;

public class WorkflowTask {

    private final String name;
    private TaskStatus status;
    private int attemptCount;
    private String errorMessage;

    public WorkflowTask(String name) {
        this.name = name;
        this.status = TaskStatus.PENDING;
        this.attemptCount = 0;
    }

    public String getName() {
        return name;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(
            TaskStatus status
    ) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(
            String errorMessage
    ) {
        this.errorMessage = errorMessage;
    }

    public void rollback() {
        if (status == TaskStatus.SUCCESS) {
            status = TaskStatus.ROLLED_BACK;
        }
    }
}