package io.scenariolens.report;

import io.scenariolens.matrix.ScenarioRow;
import java.util.List;

public class GapReport {
    private final String className;
    private final String methodName;
    private final int totalScenarios;
    private final int coveredScenarios;
    private final int scenarioCoveragePercent;
    private final int assertionStrengthPercent;
    private final List<ScenarioRow> missingScenarios;
    private final List<ScenarioRow> coveredRows;

    public GapReport(String className, String methodName, int totalScenarios, int coveredScenarios, int scenarioCoveragePercent, int assertionStrengthPercent, List<ScenarioRow> missingScenarios, List<ScenarioRow> coveredRows) {
        this.className = className;
        this.methodName = methodName;
        this.totalScenarios = totalScenarios;
        this.coveredScenarios = coveredScenarios;
        this.scenarioCoveragePercent = scenarioCoveragePercent;
        this.assertionStrengthPercent = assertionStrengthPercent;
        this.missingScenarios = missingScenarios;
        this.coveredRows = coveredRows;
    }

    public String getClassName() { return className; }
    public String getMethodName() { return methodName; }
    public int getTotalScenarios() { return totalScenarios; }
    public int getCoveredScenarios() { return coveredScenarios; }
    public int getScenarioCoveragePercent() { return scenarioCoveragePercent; }
    public int getAssertionStrengthPercent() { return assertionStrengthPercent; }
    public List<ScenarioRow> getMissingScenarios() { return missingScenarios; }
    public List<ScenarioRow> getCoveredRows() { return coveredRows; }
}
