package com.agentic.shortener.orchestration.graph;

import java.util.ArrayList;
import java.util.List;

public class WorkflowGraph {

    private final List<WorkflowNode> nodes =
            new ArrayList<>();

    public void addNode(
            WorkflowNode node
    ) {
        nodes.add(node);
    }

    public List<WorkflowNode> getNodes() {
        return nodes;
    }
}