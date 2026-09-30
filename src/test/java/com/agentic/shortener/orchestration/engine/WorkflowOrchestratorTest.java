package com.agentic.shortener.orchestration.engine;

import com.agentic.shortener.orchestration.approval.ApprovalService;
import com.agentic.shortener.orchestration.audit.AuditService;
import com.agentic.shortener.orchestration.policy.WorkflowPolicyEngine;
import com.agentic.shortener.orchestration.state.TaskStatus;
import com.agentic.shortener.orchestration.state.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowOrchestratorTest {

    private ApprovalService approvalService;
    private WorkflowStore workflowStore;
    private AuditService auditService;
    private WorkflowPolicyEngine workflowPolicyEngine;
    private WorkflowOrchestrator workflowOrchestrator;
    private WorkflowReplanningService workflowReplanningService;

    @BeforeEach
    void setUp() {

        approvalService =
                new ApprovalService();

        workflowStore =
                new WorkflowStore();

        auditService =
                new AuditService();

        workflowPolicyEngine =
                new WorkflowPolicyEngine();
        workflowReplanningService =
                new WorkflowReplanningService();

        workflowOrchestrator =
                new WorkflowOrchestrator(
                        approvalService,
                        workflowStore,
                        auditService,
                        workflowPolicyEngine,
                        workflowReplanningService
                );
    }

    @Test
    void shouldExecuteWorkflowAndWaitForApproval() {

        WorkflowExecution workflow =
                workflowOrchestrator.executeWorkflow();

        assertNotNull(workflow);
        assertNotNull(workflow.getWorkflowId());

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                workflow.getStatus()
        );

        assertEquals(
                5,
                workflow.getTasks().size()
        );

        assertTrue(
                workflow.getTasks()
                        .stream()
                        .anyMatch(task ->
                                "Requirement Analysis"
                                        .equals(task.getName())
                                        && task.getStatus()
                                        == TaskStatus.SUCCESS
                        )
        );

        assertTrue(
                workflow.getTasks()
                        .stream()
                        .anyMatch(task ->
                                "Task Decomposition"
                                        .equals(task.getName())
                                        && task.getStatus()
                                        == TaskStatus.SUCCESS
                        )
        );

        assertTrue(
                workflow.getTasks()
                        .stream()
                        .anyMatch(task ->
                                "Implementation"
                                        .equals(task.getName())
                                        && task.getStatus()
                                        == TaskStatus.SUCCESS
                        )
        );

        assertTrue(
                workflow.getTasks()
                        .stream()
                        .anyMatch(task ->
                                "Testing"
                                        .equals(task.getName())
                                        && task.getStatus()
                                        == TaskStatus.SUCCESS
                        )
        );

        assertTrue(
                workflow.getTasks()
                        .stream()
                        .anyMatch(task ->
                                "Security Review"
                                        .equals(task.getName())
                                        && task.getStatus()
                                        == TaskStatus.SUCCESS
                        )
        );
    }

    @Test
    void testingShouldRetryAndSucceed() {

        WorkflowExecution workflow =
                workflowOrchestrator.executeWorkflow();

        WorkflowTask testingTask =
                workflow.getTasks()
                        .stream()
                        .filter(task ->
                                "Testing".equals(task.getName())
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                TaskStatus.SUCCESS,
                testingTask.getStatus()
        );

        assertEquals(
                2,
                testingTask.getAttemptCount()
        );
    }

    @Test
    void securityReviewShouldExecuteSuccessfully() {

        WorkflowExecution workflow =
                workflowOrchestrator.executeWorkflow();

        WorkflowTask securityTask =
                workflow.getTasks()
                        .stream()
                        .filter(task ->
                                "Security Review"
                                        .equals(task.getName())
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                TaskStatus.SUCCESS,
                securityTask.getStatus()
        );

        assertEquals(
                1,
                securityTask.getAttemptCount()
        );
    }

    @Test
    void shouldStoreWorkflowExecution() {

        WorkflowExecution createdWorkflow =
                workflowOrchestrator.executeWorkflow();

        WorkflowExecution storedWorkflow =
                workflowOrchestrator.getWorkflow(
                        createdWorkflow.getWorkflowId()
                );

        assertEquals(
                createdWorkflow.getWorkflowId(),
                storedWorkflow.getWorkflowId()
        );

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                storedWorkflow.getStatus()
        );
    }

    @Test
    void shouldCreateApprovalRequest() {

        WorkflowExecution workflow =
                workflowOrchestrator.executeWorkflow();

        assertNotNull(
                workflow.getApprovalReason()
        );

        assertFalse(
                workflow.getApprovalReason().isBlank()
        );

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                workflow.getStatus()
        );
    }

    @Test
    void shouldRecordAuditEvents() {

        WorkflowExecution workflow =
                workflowOrchestrator.executeWorkflow();

        assertFalse(
                auditService
                        .getEvents(
                                workflow.getWorkflowId()
                        )
                        .isEmpty()
        );

        assertTrue(
                auditService
                        .getEvents(
                                workflow.getWorkflowId()
                        )
                        .stream()
                        .anyMatch(event ->
                                event.getEventType()
                                        .name()
                                        .equals(
                                                "POLICY_VALIDATION_PASSED"
                                        )
                        )
        );

        assertTrue(
                auditService
                        .getEvents(
                                workflow.getWorkflowId()
                        )
                        .stream()
                        .anyMatch(event ->
                                event.getEventType()
                                        .name()
                                        .equals(
                                                "APPROVAL_REQUESTED"
                                        )
                        )
        );
    }
}