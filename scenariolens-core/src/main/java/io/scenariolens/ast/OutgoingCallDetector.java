package io.scenariolens.ast;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.List;

public class OutgoingCallDetector {

    public List<CallNode> detect(MethodDeclaration method) {
        List<CallNode> callNodes = new ArrayList<>();

        method.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                ResolvedMethodDeclaration resolved = call.resolve();
                String declaringType = resolved.declaringType().getQualifiedName();
                String methodName = resolved.getName();
                
                ResolvedType returnTypeResolved = resolved.getReturnType();
                String returnType = returnTypeResolved.describe();
                
                // Skip static calls (e.g. RefundResponse.success()) — not mockable dependencies
                if (resolved.isStatic()) return;
                
                String variableName = extractVariableName(call);

                // Skip calls on local method-scope variables that are NOT injected fields
                // (e.g. exception catch variables like "e.getCode()")
                // Heuristic: if variableName is empty or a single-char, skip (likely a catch param)
                if (variableName.isEmpty() || variableName.length() == 1) return;

                CallNode.CallCategory category = classifyCall(declaringType, methodName);
                
                if (category != CallNode.CallCategory.UNKNOWN) {
                    CallNode node = new CallNode(call, variableName, methodName, declaringType, returnType, category);
                    node.setAssignedTo(inferAssignedTo(call));
                    callNodes.add(node);
                }

            } catch (Exception e) {
                // Fallback: use heuristic detection by variable name scope when resolution is unavailable
                // (e.g. in unit tests without a full type solver, or unresolvable external types)
                String variableName = extractVariableName(call);
                String methodName = call.getNameAsString();
                
                if (!variableName.isEmpty()) {
                    // Infer return type from the assignment target in the parent expression/statement
                    String returnType = inferReturnType(call);
                    CallNode.CallCategory category = classifyByHeuristic(variableName, methodName);
                    if (category != CallNode.CallCategory.UNKNOWN) {
                        CallNode node = new CallNode(call, variableName, methodName, variableName + "Type", returnType, category);
                        node.setAssignedTo(inferAssignedTo(call));
                        callNodes.add(node);
                    }
                }
            }
        });

        return callNodes;
    }

    private String extractVariableName(MethodCallExpr call) {
        if (call.getScope().isPresent()) {
            Expression scope = call.getScope().get();
            if (scope.isNameExpr()) {
                return scope.asNameExpr().getNameAsString();
            } else if (scope.isFieldAccessExpr()) {
                return scope.asFieldAccessExpr().getNameAsString();
            }
        }
        return "";
    }

    private String inferAssignedTo(MethodCallExpr call) {
        com.github.javaparser.ast.Node parent = call.getParentNode().orElse(null);
        while (parent != null) {
            if (parent instanceof com.github.javaparser.ast.stmt.ExpressionStmt) {
                com.github.javaparser.ast.expr.Expression expr = ((com.github.javaparser.ast.stmt.ExpressionStmt) parent).getExpression();
                if (expr instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr) {
                    com.github.javaparser.ast.expr.VariableDeclarationExpr vde = (com.github.javaparser.ast.expr.VariableDeclarationExpr) expr;
                    if (!vde.getVariables().isEmpty()) {
                        return vde.getVariables().get(0).getNameAsString();
                    }
                }
                break;
            }
            parent = parent.getParentNode().orElse(null);
        }
        return "";
    }

    private String inferReturnType(MethodCallExpr call) {
        // Walk up to the variable declaration statement to infer return type
        com.github.javaparser.ast.Node parent = call.getParentNode().orElse(null);
        while (parent != null) {
            if (parent instanceof com.github.javaparser.ast.stmt.ExpressionStmt) {
                com.github.javaparser.ast.expr.Expression expr = ((com.github.javaparser.ast.stmt.ExpressionStmt) parent).getExpression();
                if (expr instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr) {
                    return ((com.github.javaparser.ast.expr.VariableDeclarationExpr) expr).getCommonType().asString();
                }
                break;
            }
            parent = parent.getParentNode().orElse(null);
        }
        return "Object";
    }

    private CallNode.CallCategory classifyByHeuristic(String variableName, String methodName) {
        String lower = variableName.toLowerCase();
        if (lower.contains("repository") || lower.contains("repo")) return CallNode.CallCategory.REPOSITORY;
        if (lower.contains("client")) return CallNode.CallCategory.HTTP_CLIENT;
        if (lower.contains("publisher") || lower.contains("event") || methodName.equals("publish")) return CallNode.CallCategory.EVENT;
        if (lower.contains("kafka") || lower.contains("producer")) return CallNode.CallCategory.KAFKA;
        if (lower.contains("logger") || lower.contains("log") || lower.contains("audit")) return CallNode.CallCategory.INTERFACE_CALL;
        if (lower.contains("service") || lower.contains("client") || lower.contains("gateway")) return CallNode.CallCategory.INTERFACE_CALL;
        return CallNode.CallCategory.UNKNOWN;
    }

    private CallNode.CallCategory classifyCall(String declaringType, String methodName) {
        if (declaringType.startsWith("java.") || declaringType.startsWith("javax.")) {
            return CallNode.CallCategory.UNKNOWN;
        }
        
        if (declaringType.contains("Repository") || declaringType.contains("org.springframework.data")) {
            return CallNode.CallCategory.REPOSITORY;
        }
        
        if (declaringType.contains("RestTemplate") || declaringType.contains("Feign") || declaringType.contains("WebClient") || declaringType.endsWith("Client")) {
            return CallNode.CallCategory.HTTP_CLIENT;
        }
        
        if (declaringType.contains("KafkaTemplate")) {
            return CallNode.CallCategory.KAFKA;
        }
        
        if (declaringType.contains("ApplicationEventPublisher") || declaringType.contains("EventPublisher") || methodName.equals("publish")) {
            return CallNode.CallCategory.EVENT;
        }
        
        // Default to interface call if it's likely an injected dependency
        return CallNode.CallCategory.INTERFACE_CALL;
    }
}
