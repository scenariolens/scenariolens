package io.scenariolens.cfg;

import com.github.javaparser.ast.Node;

import java.util.ArrayList;
import java.util.List;

public class CfgNode {
    private final NodeType type;
    private final Node astNode;
    private final List<CfgEdge> outgoingEdges = new ArrayList<>();
    
    public CfgNode(NodeType type, Node astNode) {
        this.type = type;
        this.astNode = astNode;
    }

    public NodeType getType() {
        return type;
    }

    public Node getAstNode() {
        return astNode;
    }

    public List<CfgEdge> getOutgoingEdges() {
        return outgoingEdges;
    }

    public void addEdge(CfgNode target, CfgEdge.EdgeType type, String conditionValue) {
        this.outgoingEdges.add(new CfgEdge(this, target, type, conditionValue));
    }

    public enum NodeType {
        START,
        CALL,
        DECISION,
        THROW,
        RETURN,
        END,
        MERGE
    }
}
