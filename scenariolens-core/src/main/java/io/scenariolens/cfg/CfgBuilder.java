package io.scenariolens.cfg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.List;

public class CfgBuilder {

    public CfgNode build(MethodDeclaration method) {
        CfgNode startNode = new CfgNode(CfgNode.NodeType.START, method);
        
        if (method.getBody().isPresent()) {
            BlockStmt body = method.getBody().get();
            CfgNode lastNode = processBlock(body, startNode, null);
            if (lastNode != null && lastNode.getType() != CfgNode.NodeType.RETURN && lastNode.getType() != CfgNode.NodeType.THROW) {
                lastNode.addEdge(new CfgNode(CfgNode.NodeType.END, null), CfgEdge.EdgeType.NORMAL, null);
            }
        }
        
        return startNode;
    }

    private CfgNode processBlock(BlockStmt block, CfgNode previousNode, CfgNode catchNode) {
        CfgNode current = previousNode;
        for (Statement stmt : block.getStatements()) {
            if (current == null) break;
            current = processStatement(stmt, current, catchNode);
            if (current != null && (current.getType() == CfgNode.NodeType.RETURN || current.getType() == CfgNode.NodeType.THROW)) {
                break; // Unreachable code after return/throw
            }
        }
        return current;
    }

    private CfgNode processStatement(Statement stmt, CfgNode previousNode, CfgNode catchNode) {
        if (stmt instanceof com.github.javaparser.ast.stmt.TryStmt) {
            com.github.javaparser.ast.stmt.TryStmt tryStmt = (com.github.javaparser.ast.stmt.TryStmt) stmt;
            
            CfgNode tryCatchNode = null;
            if (!tryStmt.getCatchClauses().isEmpty()) {
                tryCatchNode = new CfgNode(CfgNode.NodeType.DECISION, tryStmt.getCatchClauses().get(0));
            }
            
            CfgNode tryEnd = processBlock(tryStmt.getTryBlock(), previousNode, tryCatchNode != null ? tryCatchNode : catchNode);
            
            CfgNode mergeNode = new CfgNode(CfgNode.NodeType.MERGE, null);
            boolean merged = false;

            if (tryEnd != null && tryEnd.getType() != CfgNode.NodeType.RETURN && tryEnd.getType() != CfgNode.NodeType.THROW) {
                tryEnd.addEdge(mergeNode, CfgEdge.EdgeType.NORMAL, null);
                merged = true;
            }

            if (tryCatchNode != null) {
                CfgNode catchEnd = processBlock(tryStmt.getCatchClauses().get(0).getBody(), tryCatchNode, catchNode);
                if (catchEnd != null && catchEnd.getType() != CfgNode.NodeType.RETURN && catchEnd.getType() != CfgNode.NodeType.THROW) {
                    catchEnd.addEdge(mergeNode, CfgEdge.EdgeType.NORMAL, null);
                    merged = true;
                }
            }
            
            return merged ? mergeNode : null;
            
        } else if (stmt instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) stmt;
            CfgNode decisionNode = new CfgNode(CfgNode.NodeType.DECISION, ifStmt.getCondition());
            previousNode.addEdge(decisionNode, CfgEdge.EdgeType.NORMAL, null);
            
            CfgNode trueStart = new CfgNode(CfgNode.NodeType.MERGE, null);
            decisionNode.addEdge(trueStart, CfgEdge.EdgeType.CONDITION_TRUE, null);
            CfgNode trueEnd = null;
            if (ifStmt.getThenStmt() instanceof BlockStmt) {
                trueEnd = processBlock((BlockStmt) ifStmt.getThenStmt(), trueStart, catchNode);
            } else {
                trueEnd = processStatement(ifStmt.getThenStmt(), trueStart, catchNode);
            }
            
            CfgNode falseStart = new CfgNode(CfgNode.NodeType.MERGE, null);
            decisionNode.addEdge(falseStart, CfgEdge.EdgeType.CONDITION_FALSE, null);
            CfgNode falseEnd = null;
            if (ifStmt.getElseStmt().isPresent()) {
                if (ifStmt.getElseStmt().get() instanceof BlockStmt) {
                    falseEnd = processBlock((BlockStmt) ifStmt.getElseStmt().get(), falseStart, catchNode);
                } else {
                    falseEnd = processStatement(ifStmt.getElseStmt().get(), falseStart, catchNode);
                }
            } else {
                falseEnd = falseStart;
            }
            
            CfgNode mergeNode = new CfgNode(CfgNode.NodeType.MERGE, null);
            boolean merged = false;
            
            if (trueEnd != null && trueEnd.getType() != CfgNode.NodeType.RETURN && trueEnd.getType() != CfgNode.NodeType.THROW) {
                trueEnd.addEdge(mergeNode, CfgEdge.EdgeType.NORMAL, null);
                merged = true;
            }
            if (falseEnd != null && falseEnd.getType() != CfgNode.NodeType.RETURN && falseEnd.getType() != CfgNode.NodeType.THROW) {
                falseEnd.addEdge(mergeNode, CfgEdge.EdgeType.NORMAL, null);
                merged = true;
            }
            
            return merged ? mergeNode : null;
            
        } else if (stmt instanceof com.github.javaparser.ast.stmt.SwitchStmt) {
            com.github.javaparser.ast.stmt.SwitchStmt switchStmt = (com.github.javaparser.ast.stmt.SwitchStmt) stmt;
            CfgNode decisionNode = new CfgNode(CfgNode.NodeType.DECISION, switchStmt.getSelector());
            previousNode.addEdge(decisionNode, CfgEdge.EdgeType.NORMAL, null);
            
            CfgNode mergeNode = new CfgNode(CfgNode.NodeType.MERGE, null);
            boolean merged = false;
            
            for (com.github.javaparser.ast.stmt.SwitchEntry entry : switchStmt.getEntries()) {
                CfgNode branchStart = new CfgNode(CfgNode.NodeType.MERGE, null);
                
                String labelStr = "default";
                if (!entry.getLabels().isEmpty()) {
                    labelStr = entry.getLabels().get(0).toString().replace("\"", "");
                }
                
                decisionNode.addEdge(branchStart, CfgEdge.EdgeType.CONDITION_TRUE, labelStr);
                
                CfgNode branchEnd = branchStart;
                for (Statement entryStmt : entry.getStatements()) {
                    if (branchEnd == null) break;
                    if (entryStmt instanceof com.github.javaparser.ast.stmt.BreakStmt) {
                        branchEnd.addEdge(mergeNode, CfgEdge.EdgeType.NORMAL, null);
                        merged = true;
                        branchEnd = null;
                        break;
                    }
                    branchEnd = processStatement(entryStmt, branchEnd, catchNode);
                    if (branchEnd != null && (branchEnd.getType() == CfgNode.NodeType.RETURN || branchEnd.getType() == CfgNode.NodeType.THROW)) {
                        break;
                    }
                }
                
                if (branchEnd != null && branchEnd != branchStart) {
                    branchEnd.addEdge(mergeNode, CfgEdge.EdgeType.NORMAL, null);
                    merged = true;
                }
            }
            
            return merged ? mergeNode : null;
            
        } else if (stmt instanceof ThrowStmt) {
            List<MethodCallExpr> calls = stmt.findAll(MethodCallExpr.class);
            CfgNode current = previousNode;
            for (MethodCallExpr call : calls) {
                CfgNode callNode = new CfgNode(CfgNode.NodeType.CALL, call);
                current.addEdge(callNode, CfgEdge.EdgeType.NORMAL, null);
                if (catchNode != null) {
                    callNode.addEdge(catchNode, CfgEdge.EdgeType.EXCEPTION, null);
                } else {
                    CfgNode methodExit = new CfgNode(CfgNode.NodeType.THROW, null);
                    callNode.addEdge(methodExit, CfgEdge.EdgeType.EXCEPTION, null);
                }
                current = callNode;
            }
            CfgNode throwNode = new CfgNode(CfgNode.NodeType.THROW, stmt);
            current.addEdge(throwNode, CfgEdge.EdgeType.NORMAL, null);
            return throwNode;
        } else if (stmt instanceof ReturnStmt) {
            List<MethodCallExpr> calls = stmt.findAll(MethodCallExpr.class);
            CfgNode current = previousNode;
            for (MethodCallExpr call : calls) {
                CfgNode callNode = new CfgNode(CfgNode.NodeType.CALL, call);
                current.addEdge(callNode, CfgEdge.EdgeType.NORMAL, null);
                if (catchNode != null) {
                    callNode.addEdge(catchNode, CfgEdge.EdgeType.EXCEPTION, null);
                } else {
                    CfgNode methodExit = new CfgNode(CfgNode.NodeType.THROW, null);
                    callNode.addEdge(methodExit, CfgEdge.EdgeType.EXCEPTION, null);
                }
                current = callNode;
            }
            CfgNode returnNode = new CfgNode(CfgNode.NodeType.RETURN, stmt);
            current.addEdge(returnNode, CfgEdge.EdgeType.NORMAL, null);
            return returnNode;
        } else {
            List<MethodCallExpr> calls = stmt.findAll(MethodCallExpr.class);
            CfgNode current = previousNode;
            for (MethodCallExpr call : calls) {
                CfgNode callNode = new CfgNode(CfgNode.NodeType.CALL, call);
                current.addEdge(callNode, CfgEdge.EdgeType.NORMAL, null);
                
                if (catchNode != null) {
                    callNode.addEdge(catchNode, CfgEdge.EdgeType.EXCEPTION, null);
                } else {
                    CfgNode methodExit = new CfgNode(CfgNode.NodeType.THROW, null);
                    callNode.addEdge(methodExit, CfgEdge.EdgeType.EXCEPTION, null);
                }
                
                current = callNode;
            }
            return current;
        }
    }
}
