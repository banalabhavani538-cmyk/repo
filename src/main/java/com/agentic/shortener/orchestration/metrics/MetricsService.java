package com.agentic.shortener.orchestration.metrics;

import com.agentic.shortener.orchestration.engine.WorkflowStore;
import com.agentic.shortener.orchestration.state.WorkflowStatus;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final WorkflowStore workflowStore;

    public MetricsService(WorkflowStore workflowStore) {
        this.workflowStore = workflowStore;
    }

    public WorkflowMetrics getMetrics() {

        var workflows = workflowStore.getAll();

        long completed = workflows.stream()
                .filter(w -> w.getStatus() == WorkflowStatus.COMPLETED)
                .count();

        long waiting = workflows.stream()
                .filter(w -> w.getStatus() == WorkflowStatus.WAITING_FOR_APPROVAL)
                .count();

        long rolledBack = workflows.stream()
                .filter(w -> w.getStatus() == WorkflowStatus.ROLLED_BACK)
                .count();

        long rejected = workflows.stream()
                .filter(w -> w.getStatus() == WorkflowStatus.REJECTED)
                .count();

        return new WorkflowMetrics(
                workflows.size(),
                completed,
                waiting,
                rolledBack,
                rejected
        );
    }
}