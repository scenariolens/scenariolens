package io.scenariolens.gap;

import io.scenariolens.matrix.ScenarioRow;
import io.scenariolens.report.GapReport;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GapAnalyzer {

    private final MockitoStubExtractor extractor = new MockitoStubExtractor();
    private final AssertionClassifier classifier = new AssertionClassifier();

    public GapReport analyze(String className, String methodName, List<ScenarioRow> matrix, List<MethodDeclaration> testMethods) {
        List<ScenarioRow> covered = new ArrayList<>();
        List<ScenarioRow> missing = new ArrayList<>(matrix);
        int strongCount = 0;

        for (MethodDeclaration test : testMethods) {
            Map<String, String> stubs = extractor.extractStubs(test);
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

        int coveragePercent = matrix.isEmpty() ? 100 : (int) ((covered.size() * 100.0) / matrix.size());
        int strengthPercent = covered.isEmpty() ? 0 : (int) ((strongCount * 100.0) / covered.size());

        return new GapReport(className, methodName, matrix.size(), covered.size(), coveragePercent, strengthPercent, missing, covered);
    }

    private ScenarioRow matchRow(List<ScenarioRow> matrix, Map<String, String> stubs) {
        for (ScenarioRow row : matrix) {
            boolean matches = true;
            for (Map.Entry<String, String> entry : stubs.entrySet()) {
                String stubVal = entry.getValue().replace("Order.builder().status(", "").replace(").build()", "");
                boolean foundMatch = row.getStubs().stream().anyMatch(
                        v -> v.getCallNode().getMethodName().equals(entry.getKey()) &&
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
