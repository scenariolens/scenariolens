package io.scenariolens.ast;

import com.github.javaparser.ast.expr.MethodCallExpr;

public class CallNode {
    private final MethodCallExpr methodCallExpr;
    private final String variableName;  // the object the method is called on (e.g. "orderRepository")
    private final String methodName;
    private final String declaringType;
    private final String returnType;
    private final CallCategory category;
    private String assignedTo = "";    // local variable the return value is assigned to (e.g. "order")
    private int occurrenceIndex = 0;

    public CallNode(MethodCallExpr methodCallExpr, String variableName, String methodName, String declaringType, String returnType, CallCategory category) {
        this.methodCallExpr = methodCallExpr;
        this.variableName = variableName;
        this.methodName = methodName;
        this.declaringType = declaringType;
        this.returnType = returnType;
        this.category = category;
    }

    public void setOccurrenceIndex(int occurrenceIndex) { this.occurrenceIndex = occurrenceIndex; }
    public int getOccurrenceIndex() { return occurrenceIndex; }
    public String getUniqueKey() {
        return methodName + (occurrenceIndex > 0 ? "[" + occurrenceIndex + "]" : "");
    }

    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedTo() { return assignedTo; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public MethodCallExpr getMethodCallExpr() {
        return methodCallExpr;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDeclaringType() {
        return declaringType;
    }

    public String getReturnType() {
        return returnType;
    }

    public CallCategory getCategory() {
        return category;
    }

    public enum CallCategory {
        REPOSITORY,
        HTTP_CLIENT,
        KAFKA,
        EVENT,
        INTERFACE_CALL,
        UNKNOWN
    }
}
