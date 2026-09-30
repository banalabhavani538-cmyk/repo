package com.agentic.shortener.orchestration.policy;

public class RetryPolicy {

    private final int maxAttempts;

    public RetryPolicy(int maxAttempts) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be at least 1"
            );
        }

        this.maxAttempts = maxAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public boolean shouldRetry(int currentAttempt) {
        return currentAttempt < maxAttempts;
    }
}