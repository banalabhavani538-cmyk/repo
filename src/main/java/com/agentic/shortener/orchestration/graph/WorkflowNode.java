package com.agentic.shortener.orchestration.graph;

import java.util.List;

public class WorkflowNode {

    private final String name;
    private final List<String> dependencies;

    public WorkflowNode(
            String name,
            List<String> dependencies
    ) {
        this.name = name;
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}