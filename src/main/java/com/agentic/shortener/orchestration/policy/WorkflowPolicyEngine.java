package com.agentic.shortener.orchestration.policy;

import com.agentic.shortener.orchestration.engine.WorkflowExecution;
import com.agentic.shortener.orchestration.state.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkflowPolicyEngine {

    private static final int MAXIMUM_TASK_COUNT = 10;

    private static final Set<String> ALLOWED_TASKS =
            Set.of(
                    "Requirement Analysis",
                    "Task Decomposition",
                    "Implementation",
                    "Testing",
                    "Security Review",
                    "Test Remediation",
                    "Security Remediation",
                    "Documentation",
                    "Production Release"
            );

    private static final Set<String> HIGH_IMPACT_TASKS =
            Set.of(
                    "Documentation",
                    "Production Release",
                    "Database Migration"
            );

    public PolicyResult validateWorkflowStart(
            WorkflowExecution workflow
    ) {

        if (workflow == null) {

            return PolicyResult.blocked(
                    "Workflow Input Policy",
                    "Workflow execution cannot be null"
            );
        }

        if (workflow.getWorkflowId() == null
                || workflow.getWorkflowId().isBlank()) {

            return PolicyResult.blocked(
                    "Workflow Identity Policy",
                    "Workflow ID is required"
            );
        }

        return PolicyResult.allowed(
                "Workflow Start Policy",
                "Workflow passed start-policy validation"
        );
    }

    public PolicyResult validateTaskExecution(
            WorkflowExecution workflow,
            String taskName
    ) {

        if (workflow == null) {

            return PolicyResult.blocked(
                    "Workflow Input Policy",
                    "Workflow execution cannot be null"
            );
        }

        if (taskName == null
                || taskName.isBlank()) {

            return PolicyResult.blocked(
                    "Task Name Policy",
                    "Task name cannot be empty"
            );
        }

        if (!ALLOWED_TASKS.contains(taskName)) {

            return PolicyResult.blocked(
                    "Task Allowlist Policy",
                    "Task is not permitted by workflow policy: "
                            + taskName
            );
        }

        if (workflow.getTasks().size()
                >= MAXIMUM_TASK_COUNT) {

            return PolicyResult.blocked(
                    "Task Limit Policy",
                    "Workflow exceeded the maximum task limit of "
                            + MAXIMUM_TASK_COUNT
            );
        }

        return PolicyResult.allowed(
                "Task Execution Policy",
                "Task is allowed to execute: "
                        + taskName
        );
    }

    public PolicyResult validateDocumentationGate(
            WorkflowExecution workflow
    ) {

        if (workflow == null) {

            return PolicyResult.blocked(
                    "Documentation Entry Gate",
                    "Workflow execution cannot be null"
            );
        }

        Set<String> successfulTasks =
                workflow.getTasks()
                        .stream()
                        .filter(task ->
                                task.getStatus()
                                        == TaskStatus.SUCCESS
                        )
                        .map(task ->
                                task.getName()
                        )
                        .collect(
                                Collectors.toSet()
                        );

        boolean testingSucceeded =
                successfulTasks.contains(
                        "Testing"
                );

        boolean securityReviewSucceeded =
                successfulTasks.contains(
                        "Security Review"
                );

        if (!testingSucceeded
                || !securityReviewSucceeded) {

            return PolicyResult.blocked(
                    "Documentation Entry Gate",
                    "Documentation requires successful Testing "
                            + "and Security Review"
            );
        }

        return PolicyResult.allowed(
                "Documentation Entry Gate",
                "Testing and Security Review completed successfully"
        );
    }

    public PolicyResult validateHighImpactAction(
            String taskName,
            boolean approvalGranted
    ) {

        if (taskName == null
                || taskName.isBlank()) {

            return PolicyResult.blocked(
                    "Change Control Policy",
                    "High-impact task name cannot be empty"
            );
        }

        boolean highImpactAction =
                HIGH_IMPACT_TASKS.contains(
                        taskName
                );

        if (highImpactAction
                && !approvalGranted) {

            return PolicyResult.blocked(
                    "Change Control Policy",
                    taskName
                            + " requires human approval"
            );
        }

        return PolicyResult.allowed(
                "Change Control Policy",
                "Change-control requirements were satisfied for "
                        + taskName
        );
    }
}