package io.scenariolens.cfg;

import io.scenariolens.ast.CallNode;
import io.scenariolens.matrix.StubVariation;
import java.util.ArrayList;
import java.util.List;

public class PathPruner {

    // Phase 1: Uses simple variable name matching for condition-to-stub correlation.
    // e.g. "order == null" is correlated to the stub returning null for the call
    // whose result is assigned to "order".
    // Multi-hop assignments and aliasing are out of scope until Phase 2.

    public List<List<StubVariation>> prune(com.github.javaparser.ast.body.MethodDeclaration method, List<CallNode> calls, List<List<StubVariation>> allCombinations) {
        CfgBuilder builder = new CfgBuilder();
        CfgNode startNode = builder.build(method);
        
        List<List<CfgEdge>> allPaths = new ArrayList<>();
        extractPaths(startNode, new ArrayList<>(), allPaths);

        List<List<StubVariation>> pruned = new ArrayList<>();
        
        for (List<StubVariation> combination : allCombinations) {
            boolean consistentWithAnyPath = false;
            for (List<CfgEdge> path : allPaths) {
                if (isConsistent(combination, path, calls)) {
                    consistentWithAnyPath = true;
                    break;
                }
            }
            if (consistentWithAnyPath) {
                pruned.add(combination);
            }
        }
        
        return pruned;
    }

    private void extractPaths(CfgNode current, List<CfgEdge> currentPath, List<List<CfgEdge>> allPaths) {
        if (current.getOutgoingEdges().isEmpty()) {
            allPaths.add(new ArrayList<>(currentPath));
            return;
        }
        for (CfgEdge edge : current.getOutgoingEdges()) {
            currentPath.add(edge);
            extractPaths(edge.getTarget(), currentPath, allPaths);
            currentPath.remove(currentPath.size() - 1);
        }
    }

    private boolean isConsistent(List<StubVariation> combination, List<CfgEdge> path, List<CallNode> allCalls) {
        List<CallNode> pathCalls = new ArrayList<>();
        for (CfgEdge edge : path) {
            if (edge.getTarget().getType() == CfgNode.NodeType.CALL) {
                com.github.javaparser.ast.expr.MethodCallExpr expr = (com.github.javaparser.ast.expr.MethodCallExpr) edge.getTarget().getAstNode();
                for (CallNode call : allCalls) {
                    if (call.getMethodCallExpr() == expr) {
                        pathCalls.add(call);
                        break;
                    }
                }
            }
        }

        // Rule 2: Reachability
        for (StubVariation stub : combination) {
            boolean onPath = pathCalls.contains(stub.getCallNode());
            boolean isNotCalledStub = (stub.getType() == StubVariation.VariationType.NOT_CALLED);
            
            if (onPath && isNotCalledStub) return false;
            if (!onPath && !isNotCalledStub) return false;
        }

        // Exception edges consistency
        for (int i = 0; i < path.size(); i++) {
            CfgEdge edge = path.get(i);
            if (edge.getTarget().getType() == CfgNode.NodeType.CALL) {
                com.github.javaparser.ast.expr.MethodCallExpr expr = (com.github.javaparser.ast.expr.MethodCallExpr) edge.getTarget().getAstNode();
                CallNode callNode = allCalls.stream().filter(c -> c.getMethodCallExpr() == expr).findFirst().orElse(null);
                
                if (callNode != null) {
                    StubVariation stub = combination.stream().filter(s -> s.getCallNode() == callNode).findFirst().orElse(null);
                    if (stub != null) {
                        boolean throwsException = (stub.getType() == StubVariation.VariationType.THROWS_EXCEPTION);
                        if (i + 1 < path.size()) {
                            CfgEdge nextEdge = path.get(i + 1);
                            boolean isExceptionEdge = (nextEdge.getType() == CfgEdge.EdgeType.EXCEPTION);
                            if (throwsException && !isExceptionEdge) return false;
                            if (!throwsException && isExceptionEdge) return false;
                        }
                    }
                }
            }
        }
        
        // Rule 3: Data Flow / Branch condition evaluation
        for (CfgEdge edge : path) {
            if (edge.getSource().getType() == CfgNode.NodeType.DECISION) {
                String cond = edge.getSource().getAstNode().toString();
                boolean isTrueEdge = (edge.getType() == CfgEdge.EdgeType.CONDITION_TRUE);
                
                if (edge.getConditionValue() != null) {
                    String caseLabel = edge.getConditionValue();
                    for (StubVariation stub : combination) {
                        String varName = stub.getCallNode().getVariableName();
                        String assignedTo = stub.getCallNode().getAssignedTo();
                        boolean condMatches = (!varName.isEmpty() && cond.contains(varName))
                                           || (!assignedTo.isEmpty() && cond.contains(assignedTo));
                        if (condMatches) {
                            String val = stub.getExactValue();
                            if (val != null) {
                                boolean isExplicitEnum = stub.getType() == StubVariation.VariationType.ENUM_CONSTANT;
                                if (isExplicitEnum || val.contains("status=")) {
                                    if (!caseLabel.equals("default") && !val.contains(caseLabel)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    boolean matchesNull = false;
                    for (StubVariation stub : combination) {
                        // Phase 1: match condition variable to either the dependency name (variableName)
                        // or the local variable the return value was assigned to (assignedTo)
                        String varName = stub.getCallNode().getVariableName();
                        String assignedTo = stub.getCallNode().getAssignedTo();
                        boolean condMatchesVar = (!varName.isEmpty() && cond.contains(varName))
                                             || (!assignedTo.isEmpty() && cond.contains(assignedTo));
                        if (condMatchesVar) {
                            if (stub.getType() == StubVariation.VariationType.NULL_RETURN) {
                                // Check against the assigned local variable (e.g. "order")
                                String checkName = assignedTo.isEmpty() ? varName : assignedTo;
                                if (cond.contains(checkName + " == null") || cond.contains("null == " + checkName) || cond.contains(checkName + "==null")) {
                                    matchesNull = true;
                                }
                            }
                        }
                    }
                    
                    if (cond.contains("||")) {
                        if (matchesNull && !isTrueEdge) return false;
                        boolean isOrNullCheck = cond.contains("== null");
                        if (isOrNullCheck && isTrueEdge && !matchesNull) return false;
                    } else {
                        for (StubVariation stub : combination) {
                            String varName = stub.getCallNode().getVariableName();
                            String assignedTo2 = stub.getCallNode().getAssignedTo();
                            // Match condition on the dependency var or the local variable holding the return value
                            boolean condMatches = (!varName.isEmpty() && cond.contains(varName))
                                              || (!assignedTo2.isEmpty() && cond.contains(assignedTo2));
                            String checkName = assignedTo2.isEmpty() ? varName : assignedTo2;
                            if (condMatches) {
                                if (stub.getType() == StubVariation.VariationType.NULL_RETURN) {
                                    // null return → must take true branch of "x == null", false branch of "x != null"
                                    boolean isNullCheck = cond.contains(checkName + " == null") || cond.contains("null == " + checkName);
                                    boolean isNotNullCheck = cond.contains(checkName + " != null") || cond.contains("null != " + checkName);
                                    if (isNullCheck && !isTrueEdge) return false;   // order==null, must take true branch
                                    if (isNotNullCheck && isTrueEdge) return false; // order!=null, must take false branch
                                } else {
                                    // non-null return → must take false branch of "x == null", true branch of "x != null"
                                    boolean isNullCheck = cond.contains(checkName + " == null") || cond.contains("null == " + checkName);
                                    boolean isNotNullCheck = cond.contains(checkName + " != null") || cond.contains("null != " + checkName);
                                    if (isNullCheck && isTrueEdge) return false;    // order!=null, must take false branch
                                    if (isNotNullCheck && !isTrueEdge) return false; // order!=null, must take true branch
                                }
                                
                                String val = stub.getExactValue();
                                if (val != null) {
                                    if (cond.contains("\"DELIVERED\"") || cond.contains("DELIVERED")) {
                                        // Only filter if stub explicitly names an enum constant like DELIVERED, CANCELLED, PENDING
                                        // Generic VALID_OBJECT stubs (e.g. "Order(valid)") are ambiguous — allow both branches
                                        boolean isExplicitEnum = stub.getType() == StubVariation.VariationType.ENUM_CONSTANT;
                                        if (isExplicitEnum) {
                                            boolean isDelivered = val.contains("DELIVERED");
                                            if (cond.contains("!")) {
                                                if (isDelivered && isTrueEdge) return false;
                                                if (!isDelivered && !isTrueEdge) return false;
                                            } else {
                                                if (isDelivered && !isTrueEdge) return false;
                                                if (!isDelivered && isTrueEdge) return false;
                                            }
                                        }
                                    }
                                    
                                    if (cond.contains("isSuccess")) {
                                        // Only filter when the stub explicitly declares boolean outcome
                                        // VALID_OBJECT stubs can take either branch (success or failure)
                                        if (stub.getType() == StubVariation.VariationType.BOOLEAN_TRUE) {
                                            if (cond.contains("!") && isTrueEdge) return false;
                                            if (!cond.contains("!") && !isTrueEdge) return false;
                                        } else if (stub.getType() == StubVariation.VariationType.BOOLEAN_FALSE) {
                                            if (cond.contains("!") && !isTrueEdge) return false;
                                            if (!cond.contains("!") && isTrueEdge) return false;
                                        }
                                        // For VALID_OBJECT / COMPLETES_NORMALLY: allow both branches
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return true;
    }
}
