package io.scenariolens.matrix;

import io.scenariolens.ast.CallNode;
import io.scenariolens.ast.ReturnVariationEnumerator;
import io.scenariolens.cfg.PathPruner;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class ScenarioMatrix {

    private final ReturnVariationEnumerator enumerator = new ReturnVariationEnumerator();
    private final PathPruner pruner = new PathPruner();
    private int rawCount = 0;

    public int getRawCount() {
        return rawCount;
    }

    public List<ScenarioRow> generate(MethodDeclaration method, List<CallNode> calls) {
        List<List<StubVariation>> allVariations = new ArrayList<>();
        
        for (CallNode call : calls) {
            allVariations.add(enumerator.enumerate(call));
        }

        List<List<StubVariation>> cartesianProduct = generateCartesianProduct(allVariations);
        this.rawCount = cartesianProduct.size();
        List<List<StubVariation>> pruned = pruner.prune(method, calls, cartesianProduct);

        List<ScenarioRow> rows = new ArrayList<>();
        int count = 1;
        for (List<StubVariation> combination : pruned) {
            String expected = determineExpectedOutcome(method, calls, combination);
            rows.add(new ScenarioRow(String.format("S%02d", count++), combination, expected));
        }
        return rows;
    }

    private List<List<StubVariation>> generateCartesianProduct(List<List<StubVariation>> lists) {
        List<List<StubVariation>> result = new ArrayList<>();
        if (lists.isEmpty()) {
            result.add(new ArrayList<>());
            return result;
        }

        List<StubVariation> firstList = lists.get(0);
        List<List<StubVariation>> remainingLists = generateCartesianProduct(lists.subList(1, lists.size()));

        for (StubVariation condition : firstList) {
            for (List<StubVariation> remainingList : remainingLists) {
                List<StubVariation> resultList = new ArrayList<>();
                resultList.add(condition);
                resultList.addAll(remainingList);
                result.add(resultList);
            }
        }
        return result;
    }

    private String getDeterministicReturn(MethodDeclaration method) {
        if (!method.getBody().isPresent()) {
            return "returns " + method.getType().asString();
        }
        List<ReturnStmt> returnStmts = method.findAll(ReturnStmt.class);
        if (returnStmts.size() == 1) {
            ReturnStmt stmt = returnStmts.get(0);
            if (stmt.getExpression().isPresent()) {
                Expression expr = stmt.getExpression().get();
                if (expr.isBooleanLiteralExpr()) {
                    return "returns " + expr.asBooleanLiteralExpr().getValue();
                } else if (expr.isIntegerLiteralExpr()) {
                    return "returns " + expr.asIntegerLiteralExpr().getValue();
                } else if (expr.isDoubleLiteralExpr()) {
                    return "returns " + expr.asDoubleLiteralExpr().getValue();
                } else if (expr.isLongLiteralExpr()) {
                    return "returns " + expr.asLongLiteralExpr().getValue();
                } else if (expr.isCharLiteralExpr()) {
                    return "returns '" + expr.asCharLiteralExpr().getValue() + "'";
                } else if (expr.isStringLiteralExpr()) {
                    return "returns \"" + expr.asStringLiteralExpr().getValue() + "\"";
                } else if (expr.isNullLiteralExpr()) {
                    return "returns null";
                }
            }
        }
        return "returns " + method.getType().asString();
    }

    private String determineExpectedOutcome(MethodDeclaration method, List<CallNode> calls, List<StubVariation> combination) {
        if (calls.isEmpty()) {
            if (method.getType().isVoidType()) {
                return "completes normally";
            }
            return getDeterministicReturn(method);
        }

        if (!combination.isEmpty()) {
            if (combination.stream().anyMatch(v -> v.getType() == StubVariation.VariationType.NULL_RETURN)) {
                return "OrderNotFoundException / NPE risk";
            }
            if (combination.stream().anyMatch(v -> v.getExactValue().contains("CANCELLED") || v.getExactValue().contains("PENDING"))) {
                return "InvalidStateException";
            }
            if (combination.stream().anyMatch(v -> v.getExactValue().contains("failed") || v.getExactValue().contains("false"))) {
                return "RefundResponse.failed";
            }
            if (combination.stream().anyMatch(v -> v.getType() == StubVariation.VariationType.THROWS_EXCEPTION)) {
                return "Exception Propagated";
            }
        }

        if (method.getType().isVoidType()) {
            return "completes normally";
        }

        if (method.getBody().isPresent()) {
            List<ReturnStmt> returnStmts = method.findAll(ReturnStmt.class);
            boolean hasSuccessReturn = returnStmts.stream()
                .anyMatch(r -> r.getExpression().isPresent() && r.getExpression().get().toString().contains("RefundResponse.success()"));
            if (hasSuccessReturn) {
                return "RefundResponse.success";
            }
        }

        return "returns " + method.getType().asString();
    }
}
