package com.agentic.shortener.orchestration.policy;

public record PolicyResult(
        PolicyDecision decision,
        String policyName,
        String message
) {

    public boolean isAllowed() {
        return decision == PolicyDecision.ALLOWED;
    }

    public static PolicyResult allowed(
            String policyName,
            String message
    ) {
        return new PolicyResult(
                PolicyDecision.ALLOWED,
                policyName,
                message
        );
    }

    public static PolicyResult blocked(
            String policyName,
            String message
    ) {
        return new PolicyResult(
                PolicyDecision.BLOCKED,
                policyName,
                message
        );
    }
}