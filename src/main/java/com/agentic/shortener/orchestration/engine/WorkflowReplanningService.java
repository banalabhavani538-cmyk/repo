package com.agentic.shortener.orchestration.engine;

import com.agentic.shortener.orchestration.state.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkflowReplanningService {

    private static final String TESTING =
            "Testing";

    private static final String SECURITY_REVIEW =
            "Security Review";

    private static final String TEST_REMEDIATION =
            "Test Remediation";

    private static final String SECURITY_REMEDIATION =
            "Security Remediation";

    public ReplanningPlan createRecoveryPlan(
            WorkflowExecution workflow
    ) {

        if (workflow == null) {
            return ReplanningPlan.unavailable(
                    "Workflow execution was not available"
            );
        }

        Set<String> failedTasks =
                workflow.getTasks()
                        .stream()
                        .filter(task ->
                                task.getStatus()
                                        == TaskStatus.FAILED
                        )
                        .map(WorkflowTask::getName)
                        .collect(Collectors.toSet());

        if (failedTasks.isEmpty()) {
            return ReplanningPlan.unavailable(
                    "No failed tasks were found"
            );
        }

        List<String> recoveryTasks =
                new ArrayList<>();

        if (failedTasks.contains(TESTING)) {
            recoveryTasks.add(
                    TEST_REMEDIATION
            );

            recoveryTasks.add(
                    TESTING
            );
        }

        if (failedTasks.contains(SECURITY_REVIEW)) {
            recoveryTasks.add(
                    SECURITY_REMEDIATION
            );

            recoveryTasks.add(
                    SECURITY_REVIEW
            );
        }

        if (recoveryTasks.isEmpty()) {
            return ReplanningPlan.unavailable(
                    "No approved recovery strategy exists for: "
                            + failedTasks
            );
        }

        return ReplanningPlan.recovery(
                "Recovery plan generated for failed tasks: "
                        + failedTasks,
                recoveryTasks
        );
    }
}