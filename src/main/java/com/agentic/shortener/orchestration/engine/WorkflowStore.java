package com.agentic.shortener.orchestration.engine;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowStore {

    private final Map<String, WorkflowExecution> workflows =
            new ConcurrentHashMap<>();

    public WorkflowExecution save(WorkflowExecution workflow) {
        workflows.put(workflow.getWorkflowId(), workflow);
        return workflow;
    }

    public WorkflowExecution get(String workflowId) {
        WorkflowExecution workflow = workflows.get(workflowId);

        if (workflow == null) {
            throw new IllegalArgumentException(
                    "Workflow not found: " + workflowId
            );
        }

        return workflow;
    }
    public List<WorkflowExecution> getAll() {
        return List.copyOf(workflows.values());
    }
}
