package com.agentic.shortener.orchestration.engine;

import com.agentic.shortener.orchestration.approval.ApprovalDecision;
import com.agentic.shortener.orchestration.approval.ApprovalRequest;
import com.agentic.shortener.orchestration.approval.ApprovalService;
import com.agentic.shortener.orchestration.audit.AuditEventType;
import com.agentic.shortener.orchestration.audit.AuditService;
import com.agentic.shortener.orchestration.graph.WorkflowGraph;
import com.agentic.shortener.orchestration.graph.WorkflowNode;
import com.agentic.shortener.orchestration.policy.PolicyResult;
import com.agentic.shortener.orchestration.policy.RetryPolicy;
import com.agentic.shortener.orchestration.policy.WorkflowPolicyEngine;
import com.agentic.shortener.orchestration.state.TaskStatus;
import com.agentic.shortener.orchestration.state.WorkflowStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class WorkflowOrchestrator {

    private static final String REQUIREMENT_ANALYSIS =
            "Requirement Analysis";

    private static final String TASK_DECOMPOSITION =
            "Task Decomposition";

    private static final String IMPLEMENTATION =
            "Implementation";

    private static final String TESTING =
            "Testing";

    private static final String SECURITY_REVIEW =
            "Security Review";

    private static final String TEST_REMEDIATION =
            "Test Remediation";

    private static final String SECURITY_REMEDIATION =
            "Security Remediation";

    private static final String DOCUMENTATION =
            "Documentation";

    private final ApprovalService approvalService;
    private final WorkflowStore workflowStore;
    private final AuditService auditService;
    private final WorkflowPolicyEngine workflowPolicyEngine;
    private final WorkflowReplanningService workflowReplanningService;

    private final RetryPolicy retryPolicy =
            new RetryPolicy(3);

    public WorkflowOrchestrator(
            ApprovalService approvalService,
            WorkflowStore workflowStore,
            AuditService auditService,
            WorkflowPolicyEngine workflowPolicyEngine,
            WorkflowReplanningService workflowReplanningService
    ) {
        this.approvalService = approvalService;
        this.workflowStore = workflowStore;
        this.auditService = auditService;
        this.workflowPolicyEngine = workflowPolicyEngine;
        this.workflowReplanningService =
                workflowReplanningService;
    }

    /**
     * Starts a new agentic engineering workflow.
     *
     * Execution flow:
     *
     * Requirement Analysis
     *          ↓
     * Task Decomposition
     *          ↓
     * Implementation
     *          ↓
     * Testing ───────── Security Review
     *          ↓ synchronization
     * Human Approval
     *          ↓
     * Documentation
     */
    public WorkflowExecution executeWorkflow() {

        WorkflowExecution workflow =
                new WorkflowExecution();

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.WORKFLOW_CREATED,
                "Workflow was created"
        );

        /*
         * Workflow entry policy gate.
         */
        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.POLICY_VALIDATION_STARTED,
                "Workflow start-policy validation started"
        );

        PolicyResult startPolicyResult =
                workflowPolicyEngine.validateWorkflowStart(
                        workflow
                );

        if (!startPolicyResult.isAllowed()) {

            return safeStopWorkflow(
                    workflow,
                    startPolicyResult
            );
        }

        recordPolicyPassed(
                workflow.getWorkflowId(),
                startPolicyResult
        );

        workflow.setStatus(
                WorkflowStatus.RUNNING
        );

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.WORKFLOW_STARTED,
                "Workflow execution started"
        );

        /*
         * Build the dependency graph.
         */
        buildWorkflowGraph();

        /*
         * Stage 1:
         * Requirement Analysis
         */
        executeSequentialStage(
                workflow,
                REQUIREMENT_ANALYSIS
        );

        if (shouldStopExecution(workflow)) {
            return finishStoppedWorkflow(workflow);
        }

        /*
         * Stage 2:
         * Task Decomposition
         */
        executeSequentialStage(
                workflow,
                TASK_DECOMPOSITION
        );

        if (shouldStopExecution(workflow)) {
            return finishStoppedWorkflow(workflow);
        }

        /*
         * Stage 3:
         * Implementation
         */
        executeSequentialStage(
                workflow,
                IMPLEMENTATION
        );

        if (shouldStopExecution(workflow)) {
            return finishStoppedWorkflow(workflow);
        }

        /*
         * Stage 4:
         *
         * Testing and Security Review both depend
         * on Implementation, so they can execute
         * concurrently.
         */
        executeParallelValidationStage(
                workflow
        );

        if (isSafeStopped(workflow)) {

            return workflowStore.save(
                    workflow
            );
        }

        /*
         * If parallel validation failed after all
         * retries, attempt dynamic replanning.
         */
        if (hasFailed(workflow)) {

            boolean recovered =
                    attemptDynamicReplanning(
                            workflow
                    );

            if (!recovered || hasFailed(workflow)) {

                return rollbackAndSave(
                        workflow
                );
            }
        }

        /*
         * Exit gate for the validation stage.
         *
         * Documentation cannot proceed unless
         * both Testing and Security Review succeeded.
         */
        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.POLICY_VALIDATION_STARTED,
                "Documentation entry-gate validation started"
        );

        PolicyResult documentationGateResult =
                workflowPolicyEngine
                        .validateDocumentationGate(
                                workflow
                        );

        if (!documentationGateResult.isAllowed()) {

            return safeStopWorkflow(
                    workflow,
                    documentationGateResult
            );
        }

        recordPolicyPassed(
                workflow.getWorkflowId(),
                documentationGateResult
        );

        /*
         * Documentation is treated as a controlled
         * high-impact action.
         *
         * Stop execution and request human approval.
         */
        String approvalReason =
                "Testing and Security Review completed. "
                        + "Human approval is required before "
                        + "final documentation.";

        approvalService.requestApproval(
                workflow.getWorkflowId(),
                approvalReason
        );

        workflow.setApprovalReason(
                approvalReason
        );

        workflow.setStatus(
                WorkflowStatus.WAITING_FOR_APPROVAL
        );

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.APPROVAL_REQUESTED,
                approvalReason
        );

        return workflowStore.save(
                workflow
        );
    }

    /**
     * Continues a workflow after a reviewer either
     * approves or rejects the approval request.
     */
    public WorkflowExecution continueWorkflow(
            String workflowId
    ) {

        WorkflowExecution workflow =
                workflowStore.get(
                        workflowId
                );

        if (workflow.getStatus()
                != WorkflowStatus.WAITING_FOR_APPROVAL) {

            throw new IllegalStateException(
                    "Workflow is not waiting for approval"
            );
        }

        ApprovalRequest approvalRequest =
                approvalService.getApproval(
                        workflowId
                );

        if (approvalRequest.getDecision()
                == ApprovalDecision.PENDING) {

            throw new IllegalStateException(
                    "Workflow approval is still pending"
            );
        }

        if (approvalRequest.getDecision()
                == ApprovalDecision.REJECTED) {

            workflow.setStatus(
                    WorkflowStatus.REJECTED
            );

            workflow.setApprovalReason(
                    "Workflow was rejected by the reviewer"
            );

            auditService.record(
                    workflowId,
                    AuditEventType.APPROVAL_REJECTED,
                    "Workflow approval was rejected"
            );

            return workflowStore.save(
                    workflow
            );
        }

        /*
         * Revalidate the documentation entry gate.
         *
         * This protects against stale or modified
         * workflow state while approval was pending.
         */
        auditService.record(
                workflowId,
                AuditEventType.POLICY_VALIDATION_STARTED,
                "Documentation entry gate was revalidated"
        );

        PolicyResult documentationGateResult =
                workflowPolicyEngine
                        .validateDocumentationGate(
                                workflow
                        );

        if (!documentationGateResult.isAllowed()) {

            return safeStopWorkflow(
                    workflow,
                    documentationGateResult
            );
        }

        recordPolicyPassed(
                workflowId,
                documentationGateResult
        );

        /*
         * Validate change-control policy now that
         * human approval has been granted.
         */
        PolicyResult changeControlResult =
                workflowPolicyEngine
                        .validateHighImpactAction(
                                DOCUMENTATION,
                                true
                        );

        if (!changeControlResult.isAllowed()) {

            return safeStopWorkflow(
                    workflow,
                    changeControlResult
            );
        }

        recordPolicyPassed(
                workflowId,
                changeControlResult
        );

        auditService.record(
                workflowId,
                AuditEventType.APPROVAL_GRANTED,
                "Workflow approval was granted"
        );

        workflow.setStatus(
                WorkflowStatus.RUNNING
        );

        workflow.setApprovalReason(
                null
        );

        auditService.record(
                workflowId,
                AuditEventType.WORKFLOW_RESUMED,
                "Workflow resumed after approval"
        );

        /*
         * Final controlled stage.
         */
        executeSequentialStage(
                workflow,
                DOCUMENTATION
        );

        if (isSafeStopped(workflow)) {

            return workflowStore.save(
                    workflow
            );
        }

        if (hasFailed(workflow)) {

            return rollbackAndSave(
                    workflow
            );
        }

        completeWorkflow(
                workflow
        );

        return workflowStore.save(
                workflow
        );
    }

    /**
     * Returns a stored workflow by ID.
     */
    public WorkflowExecution getWorkflow(
            String workflowId
    ) {

        return workflowStore.get(
                workflowId
        );
    }

    /**
     * Executes one stage sequentially after applying
     * task-level policy validation.
     */
    private void executeSequentialStage(
            WorkflowExecution workflow,
            String taskName
    ) {

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.POLICY_VALIDATION_STARTED,
                "Task policy validation started for "
                        + taskName
        );

        PolicyResult taskPolicyResult =
                workflowPolicyEngine
                        .validateTaskExecution(
                                workflow,
                                taskName
                        );

        if (!taskPolicyResult.isAllowed()) {

            safeStopWorkflow(
                    workflow,
                    taskPolicyResult
            );

            return;
        }

        recordPolicyPassed(
                workflow.getWorkflowId(),
                taskPolicyResult
        );

        WorkflowTask task =
                executeAndAuditTask(
                        workflow.getWorkflowId(),
                        taskName
                );

        workflow.getTasks().add(
                task
        );
    }

    /**
     * Executes Testing and Security Review in parallel.
     */
    private void executeParallelValidationStage(
            WorkflowExecution workflow
    ) {

        String workflowId =
                workflow.getWorkflowId();

        /*
         * Validate both branches before execution.
         */
        auditService.record(
                workflowId,
                AuditEventType.POLICY_VALIDATION_STARTED,
                "Parallel Testing policy validation started"
        );

        PolicyResult testingPolicy =
                workflowPolicyEngine
                        .validateTaskExecution(
                                workflow,
                                TESTING
                        );

        if (!testingPolicy.isAllowed()) {

            safeStopWorkflow(
                    workflow,
                    testingPolicy
            );

            return;
        }

        recordPolicyPassed(
                workflowId,
                testingPolicy
        );

        auditService.record(
                workflowId,
                AuditEventType.POLICY_VALIDATION_STARTED,
                "Parallel Security Review policy validation started"
        );

        PolicyResult securityPolicy =
                workflowPolicyEngine
                        .validateTaskExecution(
                                workflow,
                                SECURITY_REVIEW
                        );

        if (!securityPolicy.isAllowed()) {

            safeStopWorkflow(
                    workflow,
                    securityPolicy
            );

            return;
        }

        recordPolicyPassed(
                workflowId,
                securityPolicy
        );

        /*
         * Parallel branch 1.
         */
        CompletableFuture<WorkflowTask> testingFuture =
                CompletableFuture.supplyAsync(
                        () -> executeAndAuditTask(
                                workflowId,
                                TESTING
                        )
                );

        /*
         * Parallel branch 2.
         */
        CompletableFuture<WorkflowTask> securityFuture =
                CompletableFuture.supplyAsync(
                        () -> executeAndAuditTask(
                                workflowId,
                                SECURITY_REVIEW
                        )
                );

        try {

            /*
             * Synchronization barrier.
             *
             * Documentation cannot be considered
             * until both parallel branches finish.
             */
            CompletableFuture.allOf(
                    testingFuture,
                    securityFuture
            ).join();

            WorkflowTask testingTask =
                    testingFuture.join();

            WorkflowTask securityTask =
                    securityFuture.join();

            /*
             * Add results only after both branches
             * have completed.
             */
            workflow.getTasks().add(
                    testingTask
            );

            workflow.getTasks().add(
                    securityTask
            );

        } catch (CompletionException exception) {

            Throwable cause =
                    exception.getCause() != null
                            ? exception.getCause()
                            : exception;

            WorkflowTask failureTask =
                    new WorkflowTask(
                            "Parallel Validation"
                    );

            failureTask.setStatus(
                    TaskStatus.FAILED
            );

            failureTask.setErrorMessage(
                    cause.getMessage()
            );

            workflow.getTasks().add(
                    failureTask
            );

            auditService.record(
                    workflowId,
                    AuditEventType.TASK_FAILED,
                    "Parallel validation failed: "
                            + cause.getMessage()
            );
        }
    }

    /**
     * Executes a task and writes its final result
     * to the audit trail.
     */
    private WorkflowTask executeAndAuditTask(
            String workflowId,
            String taskName
    ) {

        auditService.record(
                workflowId,
                AuditEventType.TASK_STARTED,
                "Task started: "
                        + taskName
        );

        WorkflowTask task =
                executeTask(
                        workflowId,
                        taskName
                );

        if (task.getStatus()
                == TaskStatus.SUCCESS) {

            auditService.record(
                    workflowId,
                    AuditEventType.TASK_SUCCEEDED,
                    "Task succeeded: "
                            + taskName
                            + " after "
                            + task.getAttemptCount()
                            + " attempt(s)"
            );

        } else {

            auditService.record(
                    workflowId,
                    AuditEventType.TASK_FAILED,
                    "Task failed: "
                            + taskName
                            + ". Error: "
                            + task.getErrorMessage()
            );
        }

        return task;
    }

    /**
     * Executes one task using the configured retry policy.
     */
    private WorkflowTask executeTask(
            String workflowId,
            String taskName
    ) {

        WorkflowTask task =
                new WorkflowTask(
                        taskName
                );

        while (retryPolicy.shouldRetry(
                task.getAttemptCount()
        )) {

            task.incrementAttemptCount();

            task.setStatus(
                    TaskStatus.RUNNING
            );

            if (task.getAttemptCount() > 1) {

                auditService.record(
                        workflowId,
                        AuditEventType.TASK_RETRY,
                        "Retrying task: "
                                + taskName
                                + ", attempt: "
                                + task.getAttemptCount()
                );
            }

            try {

                runTaskLogic(
                        taskName,
                        task.getAttemptCount()
                );

                task.setStatus(
                        TaskStatus.SUCCESS
                );

                task.setErrorMessage(
                        null
                );

                return task;

            } catch (Exception exception) {

                task.setStatus(
                        TaskStatus.FAILED
                );

                task.setErrorMessage(
                        exception.getMessage()
                );
            }
        }

        return task;
    }

    /**
     * Simulated task implementation.
     *
     * Replace this method later with real agents,
     * tools, model calls, code generation, test
     * execution, or validation logic.
     */
    private void runTaskLogic(
            String taskName,
            int attemptNumber
    ) {

        /*
         * Simulates a temporary Testing failure.
         *
         * First attempt fails.
         * Second attempt succeeds.
         *
         * This demonstrates retry behavior.
         */
        if (TESTING.equals(taskName)
                && attemptNumber == 1) {

            throw new IllegalStateException(
                    "Simulated test failure"
            );
        }

        /*
         * Simulate parallel work taking time.
         *
         * Console output will show different
         * ForkJoinPool worker threads.
         */
        if (TESTING.equals(taskName)
                || SECURITY_REVIEW.equals(taskName)) {

            try {

                Thread.sleep(
                        1000
                );

            } catch (InterruptedException exception) {

                Thread.currentThread()
                        .interrupt();

                throw new IllegalStateException(
                        "Parallel task was interrupted",
                        exception
                );
            }
        }

        if (TEST_REMEDIATION.equals(taskName)) {

            System.out.println(
                    "Applying test-failure remediation"
            );
        }

        if (SECURITY_REMEDIATION.equals(taskName)) {

            System.out.println(
                    "Applying security remediation"
            );
        }

        System.out.println(
                "Successfully executed task: "
                        + taskName
                        + ", attempt: "
                        + attemptNumber
                        + ", thread: "
                        + Thread.currentThread().getName()
        );
    }

    /**
     * Attempts to recover a failed validation stage
     * by generating and executing a replacement plan.
     */
    private boolean attemptDynamicReplanning(
            WorkflowExecution workflow
    ) {

        String workflowId =
                workflow.getWorkflowId();

        auditService.record(
                workflowId,
                AuditEventType.REPLANNING_STARTED,
                "Dynamic replanning started after task failure"
        );

        ReplanningPlan replanningPlan =
                workflowReplanningService
                        .createRecoveryPlan(
                                workflow
                        );

        if (!replanningPlan.recoveryAvailable()) {

            auditService.record(
                    workflowId,
                    AuditEventType.REPLANNING_FAILED,
                    replanningPlan.reason()
            );

            return false;
        }

        /*
         * Preserve the failed task in workflow history,
         * but mark it as replaced by a new plan.
         */
        workflow.getTasks()
                .stream()
                .filter(task ->
                        task.getStatus()
                                == TaskStatus.FAILED
                )
                .forEach(task ->
                        task.setStatus(
                                TaskStatus.REPLANNED
                        )
                );

        auditService.record(
                workflowId,
                AuditEventType.FALLBACK_EXECUTED,
                "Executing fallback recovery plan: "
                        + replanningPlan.recoveryTasks()
        );

        /*
         * Execute remediation tasks and then re-run
         * the failed workflow stage.
         */
        for (String recoveryTask :
                replanningPlan.recoveryTasks()) {

            executeSequentialStage(
                    workflow,
                    recoveryTask
            );

            if (isSafeStopped(workflow)) {

                auditService.record(
                        workflowId,
                        AuditEventType.REPLANNING_FAILED,
                        "Recovery was stopped by policy while executing: "
                                + recoveryTask
                );

                return false;
            }

            if (hasFailed(workflow)) {

                auditService.record(
                        workflowId,
                        AuditEventType.REPLANNING_FAILED,
                        "Recovery task failed: "
                                + recoveryTask
                );

                return false;
            }
        }

        auditService.record(
                workflowId,
                AuditEventType.REPLANNING_COMPLETED,
                "Dynamic replanning completed successfully"
        );

        return true;
    }

    /**
     * Returns true when at least one current task
     * remains in FAILED status.
     *
     * REPLANNED tasks are historical failures and
     * therefore do not trigger rollback.
     */
    private boolean hasFailed(
            WorkflowExecution workflow
    ) {

        return workflow.getTasks()
                .stream()
                .anyMatch(task ->
                        task.getStatus()
                                == TaskStatus.FAILED
                );
    }

    private boolean isSafeStopped(
            WorkflowExecution workflow
    ) {

        return workflow.getStatus()
                == WorkflowStatus.SAFE_STOPPED;
    }

    private boolean shouldStopExecution(
            WorkflowExecution workflow
    ) {

        return isSafeStopped(workflow)
                || hasFailed(workflow);
    }

    /**
     * Handles a stopped sequential stage.
     */
    private WorkflowExecution finishStoppedWorkflow(
            WorkflowExecution workflow
    ) {

        if (isSafeStopped(workflow)) {

            return workflowStore.save(
                    workflow
            );
        }

        return rollbackAndSave(
                workflow
        );
    }

    /**
     * Safely stops a workflow after a policy violation.
     */
    private WorkflowExecution safeStopWorkflow(
            WorkflowExecution workflow,
            PolicyResult policyResult
    ) {

        workflow.setStatus(
                WorkflowStatus.SAFE_STOPPED
        );

        workflow.setApprovalReason(
                policyResult.message()
        );

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.POLICY_VIOLATION,
                policyResult.policyName()
                        + ": "
                        + policyResult.message()
        );

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.WORKFLOW_SAFE_STOPPED,
                "Workflow execution was safely stopped"
        );

        return workflowStore.save(
                workflow
        );
    }

    /**
     * Rolls back successful tasks in reverse order.
     */
    private WorkflowExecution rollbackAndSave(
            WorkflowExecution workflow
    ) {

        rollbackCompletedTasks(
                workflow
        );

        workflow.setStatus(
                WorkflowStatus.ROLLED_BACK
        );

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.WORKFLOW_ROLLED_BACK,
                "Workflow was rolled back"
        );

        return workflowStore.save(
                workflow
        );
    }

    /**
     * Compensates completed workflow tasks in reverse
     * execution order.
     */
    private void rollbackCompletedTasks(
            WorkflowExecution workflow
    ) {

        List<WorkflowTask> tasks =
                workflow.getTasks();

        for (int index =
             tasks.size() - 1;
             index >= 0;
             index--) {

            WorkflowTask task =
                    tasks.get(index);

            if (task.getStatus()
                    == TaskStatus.SUCCESS) {

                rollbackTaskLogic(
                        task.getName()
                );

                task.rollback();
            }
        }
    }

    /**
     * Placeholder compensation logic.
     */
    private void rollbackTaskLogic(
            String taskName
    ) {

        System.out.println(
                "Rolling back task: "
                        + taskName
        );
    }

    /**
     * Marks the workflow as successfully completed.
     */
    private void completeWorkflow(
            WorkflowExecution workflow
    ) {

        workflow.setApprovalReason(
                null
        );

        workflow.setStatus(
                WorkflowStatus.COMPLETED
        );

        auditService.record(
                workflow.getWorkflowId(),
                AuditEventType.WORKFLOW_COMPLETED,
                "Workflow completed successfully"
        );
    }

    /**
     * Records a successful policy decision.
     */
    private void recordPolicyPassed(
            String workflowId,
            PolicyResult policyResult
    ) {

        auditService.record(
                workflowId,
                AuditEventType.POLICY_VALIDATION_PASSED,
                policyResult.policyName()
                        + ": "
                        + policyResult.message()
        );
    }

    /**
     * Creates the explicit dependency graph.
     */
    private WorkflowGraph buildWorkflowGraph() {

        WorkflowGraph graph =
                new WorkflowGraph();

        graph.addNode(
                new WorkflowNode(
                        REQUIREMENT_ANALYSIS,
                        List.of()
                )
        );

        graph.addNode(
                new WorkflowNode(
                        TASK_DECOMPOSITION,
                        List.of(
                                REQUIREMENT_ANALYSIS
                        )
                )
        );

        graph.addNode(
                new WorkflowNode(
                        IMPLEMENTATION,
                        List.of(
                                TASK_DECOMPOSITION
                        )
                )
        );

        graph.addNode(
                new WorkflowNode(
                        TESTING,
                        List.of(
                                IMPLEMENTATION
                        )
                )
        );

        graph.addNode(
                new WorkflowNode(
                        SECURITY_REVIEW,
                        List.of(
                                IMPLEMENTATION
                        )
                )
        );

        graph.addNode(
                new WorkflowNode(
                        TEST_REMEDIATION,
                        List.of(
                                TESTING
                        )
                )
        );

        graph.addNode(
                new WorkflowNode(
                        SECURITY_REMEDIATION,
                        List.of(
                                SECURITY_REVIEW
                        )
                )
        );

        graph.addNode(
                new WorkflowNode(
                        DOCUMENTATION,
                        List.of(
                                TESTING,
                                SECURITY_REVIEW
                        )
                )
        );

        return graph;
    }
}