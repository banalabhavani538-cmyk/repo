package com.agentic.shortener.orchestration.engine;

import java.util.List;

public record ReplanningPlan(
        boolean recoveryAvailable,
        String reason,
        List<String> recoveryTasks
) {

    public static ReplanningPlan recovery(
            String reason,
            List<String> recoveryTasks
    ) {
        return new ReplanningPlan(
                true,
                reason,
                List.copyOf(recoveryTasks)
        );
    }

    public static ReplanningPlan unavailable(
            String reason
    ) {
        return new ReplanningPlan(
                false,
                reason,
                List.of()
        );
    }
}