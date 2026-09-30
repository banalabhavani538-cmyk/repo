package com.agentic.shortener.orchestration.state;

public enum TaskStatus {

    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    ROLLED_BACK,
    REPLANNED
}