package io.scenariolens.cfg;

import io.scenariolens.matrix.StubVariation;
import java.util.ArrayList;
import java.util.List;

public class ExecutionPath {
    private final List<CfgNode> nodes = new ArrayList<>();
    private final List<StubVariation> requiredStubs = new ArrayList<>();
    private CfgNode.NodeType terminalOutcome; // RETURN or THROW
    
    public void addNode(CfgNode node) {
        nodes.add(node);
    }
    
    public void addRequiredStub(StubVariation stub) {
        requiredStubs.add(stub);
    }
    
    public void setTerminalOutcome(CfgNode.NodeType terminalOutcome) {
        this.terminalOutcome = terminalOutcome;
    }

    public List<CfgNode> getNodes() {
        return nodes;
    }

    public List<StubVariation> getRequiredStubs() {
        return requiredStubs;
    }

    public CfgNode.NodeType getTerminalOutcome() {
        return terminalOutcome;
    }
}
