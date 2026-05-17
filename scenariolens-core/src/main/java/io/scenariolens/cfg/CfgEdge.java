package io.scenariolens.cfg;

public class CfgEdge {
    private final CfgNode source;
    private final CfgNode target;
    private final EdgeType type;
    private final String conditionValue;

    public CfgEdge(CfgNode source, CfgNode target, EdgeType type, String conditionValue) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.conditionValue = conditionValue;
    }

    public CfgNode getSource() {
        return source;
    }

    public CfgNode getTarget() {
        return target;
    }

    public EdgeType getType() {
        return type;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public enum EdgeType {
        NORMAL,
        CONDITION_TRUE,
        CONDITION_FALSE,
        EXCEPTION
    }
}
