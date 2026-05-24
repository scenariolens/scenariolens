package io.scenariolens.gap;

import io.scenariolens.matrix.ScenarioRow;
import io.scenariolens.report.GapReport;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class GapAnalyzer {

    private final MockitoStubExtractor extractor = new MockitoStubExtractor();
    private final AssertionClassifier classifier = new AssertionClassifier();

    public GapReport analyze(String className, String methodName, String filePath, int lineNumber, List<ScenarioRow> matrix, List<MethodDeclaration> testMethods, Map<String, Map<String, List<String>>> globalFakes) {
        List<ScenarioRow> covered = new ArrayList<>();
        List<ScenarioRow> missing = new ArrayList<>(matrix);
        int strongCount = 0;
        boolean[] usedHeuristic = new boolean[1];

        if (testMethods.isEmpty()) {
            Map<String, String> baseFakeStubs = buildFakeStubsForTest(matrix, globalFakes, false, usedHeuristic);
            if (!baseFakeStubs.isEmpty()) {
                ScenarioRow matchedRow = matchRow(matrix, baseFakeStubs);
                if (matchedRow != null && !covered.contains(matchedRow)) {
                    covered.add(matchedRow);
                    missing.remove(matchedRow);
                }
            }
        } else {
            for (MethodDeclaration test : testMethods) {
                boolean assertsException = !test.findAll(MethodCallExpr.class).stream()
                        .filter(c -> c.getNameAsString().equals("assertThrows"))
                        .collect(Collectors.toList()).isEmpty();

                Map<String, String> stubs = buildFakeStubsForTest(matrix, globalFakes, assertsException, usedHeuristic);
                stubs.putAll(extractor.extractStubs(test)); // Mockito overrides fakes
                String assertionStrength = classifier.classify(test);

                ScenarioRow matchedRow = matchRow(matrix, stubs);
                if (matchedRow != null) {
                    if (!covered.contains(matchedRow)) {
                        covered.add(matchedRow);
                        missing.remove(matchedRow);
                        if ("STRONG".equals(assertionStrength)) {
                            strongCount++;
                        }
                    }
                }
            }
        }

        int coveragePercent = matrix.isEmpty() ? 100 : (int) ((covered.size() * 100.0) / matrix.size());
        int strengthPercent = covered.isEmpty() ? 0 : (int) ((strongCount * 100.0) / covered.size());

        return new GapReport(className, methodName, filePath, lineNumber, matrix.size(), covered.size(), coveragePercent, strengthPercent, missing, covered, usedHeuristic[0]);
    }

    private Map<String, String> buildFakeStubsForTest(List<ScenarioRow> matrix, Map<String, Map<String, List<String>>> globalFakes, boolean assertsException, boolean[] usedHeuristic) {
        Map<String, String> stubs = new HashMap<>();
        for (ScenarioRow row : matrix) {
            for (io.scenariolens.matrix.StubVariation v : row.getStubs()) {
                io.scenariolens.ast.CallNode call = v.getCallNode();
                String interfaceName = call.getDeclaringType();
                if (interfaceName.contains(".")) {
                    interfaceName = interfaceName.substring(interfaceName.lastIndexOf('.') + 1);
                }
                if (globalFakes.containsKey(interfaceName)) {
                    List<String> fakeReturns = globalFakes.get(interfaceName).get(call.getMethodName());
                    if (fakeReturns != null && !fakeReturns.isEmpty()) {
                        String chosenReturn = fakeReturns.get(0);
                        if (fakeReturns.size() > 1) {
                            usedHeuristic[0] = true;
                            if (assertsException) {
                                chosenReturn = fakeReturns.stream().filter(r -> r.contains("throws")).findFirst().orElse(fakeReturns.get(0));
                            } else {
                                chosenReturn = fakeReturns.stream().filter(r -> !r.contains("throws")).findFirst().orElse(fakeReturns.get(0));
                            }
                        }
                        stubs.put(call.getUniqueKey(), chosenReturn);
                    }
                }
            }
        }
        return stubs;
    }

    private ScenarioRow matchRow(List<ScenarioRow> matrix, Map<String, String> stubs) {
        for (ScenarioRow row : matrix) {
            boolean matches = true;
            for (Map.Entry<String, String> entry : stubs.entrySet()) {
                String stubVal = entry.getValue().replace("Order.builder().status(", "").replace(").build()", "");
                boolean foundMatch = row.getStubs().stream().anyMatch(
                        v -> v.getCallNode().getUniqueKey().equals(entry.getKey()) &&
                             v.getExactValue().contains(stubVal)
                );
                if (!foundMatch && !entry.getValue().equals("null")) {
                    matches = false;
                    break;
                }
            }
            if (matches && !stubs.isEmpty()) {
                return row;
            }
        }
        return null;
    }
}
