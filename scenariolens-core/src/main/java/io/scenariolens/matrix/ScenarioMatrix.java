package io.scenariolens.matrix;

import io.scenariolens.ast.CallNode;
import io.scenariolens.ast.ReturnVariationEnumerator;
import io.scenariolens.cfg.PathPruner;

import java.util.ArrayList;
import java.util.List;

public class ScenarioMatrix {

    private final ReturnVariationEnumerator enumerator = new ReturnVariationEnumerator();
    private final PathPruner pruner = new PathPruner();
    private int rawCount = 0;

    public int getRawCount() {
        return rawCount;
    }

    public List<ScenarioRow> generate(com.github.javaparser.ast.body.MethodDeclaration method, List<CallNode> calls) {
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
            // POC heuristic to determine expected outcome based on stubs
            String expected = determineExpectedOutcome(combination);
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

    private String determineExpectedOutcome(List<StubVariation> combination) {
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
        return "RefundResponse.success";
    }
}
