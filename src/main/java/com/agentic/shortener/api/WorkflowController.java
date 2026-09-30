package com.agentic.shortener.api;

import com.agentic.shortener.orchestration.engine.WorkflowExecution;
import com.agentic.shortener.orchestration.engine.WorkflowOrchestrator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowOrchestrator workflowOrchestrator;

    public WorkflowController(
            WorkflowOrchestrator workflowOrchestrator
    ) {
        this.workflowOrchestrator =
                workflowOrchestrator;
    }

    @PostMapping
    public WorkflowExecution executeWorkflow() {
        return workflowOrchestrator.executeWorkflow();
    }

    @PostMapping("/{workflowId}/continue")
    public WorkflowExecution continueWorkflow(
            @PathVariable String workflowId
    ) {
        return workflowOrchestrator.continueWorkflow(
                workflowId
        );
    }

    @GetMapping("/{workflowId}")
    public WorkflowExecution getWorkflow(
            @PathVariable String workflowId
    ) {
        return workflowOrchestrator.getWorkflow(
                workflowId
        );
    }
}