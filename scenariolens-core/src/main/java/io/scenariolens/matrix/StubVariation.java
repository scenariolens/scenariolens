package io.scenariolens.matrix;

import io.scenariolens.ast.CallNode;

public class StubVariation {
    private final CallNode callNode;
    private final String variationDescription;
    private final VariationType type;
    private final String exactValue; // e.g., "Order(DELIVERED)", "null", "throws RuntimeException"

    public StubVariation(CallNode callNode, VariationType type, String exactValue, String variationDescription) {
        this.callNode = callNode;
        this.type = type;
        this.exactValue = exactValue;
        this.variationDescription = variationDescription;
    }

    public CallNode getCallNode() {
        return callNode;
    }

    public VariationType getType() {
        return type;
    }

    public String getExactValue() {
        return exactValue;
    }

    public String getVariationDescription() {
        return variationDescription;
    }

    public enum VariationType {
        ENUM_CONSTANT,
        NULL_RETURN,
        VALID_OBJECT,
        BOOLEAN_TRUE,
        BOOLEAN_FALSE,
        THROWS_EXCEPTION,
        COMPLETES_NORMALLY,
        NOT_CALLED,
        MUST_BE_CALLED
    }

    public String getSuggestedMockitoStub() {
        String varName = callNode.getVariableName();
        String method = callNode.getMethodName();
        if (varName == null || varName.isEmpty()) varName = "mockObj";
        
        if (type == VariationType.THROWS_EXCEPTION) {
            return "when(" + varName + "." + method + "(any())).thenThrow(new " + exactValue + "());";
        } else if (type == VariationType.NULL_RETURN) {
            return "when(" + varName + "." + method + "(any())).thenReturn(null);";
        } else if (type == VariationType.VALID_OBJECT) {
            String rawType = callNode.getReturnType();
            if (rawType != null && rawType.contains("<")) {
                rawType = rawType.substring(0, rawType.indexOf('<'));
            }
            if (rawType == null) rawType = "Object";
            return "when(" + varName + "." + method + "(any())).thenReturn(org.mockito.Mockito.mock(" + rawType + ".class));";
        } else {
            return "when(" + varName + "." + method + "(any())).thenReturn(" + exactValue + ");";
        }
    }
    
    @Override
    public String toString() {
        return exactValue;
    }
}
