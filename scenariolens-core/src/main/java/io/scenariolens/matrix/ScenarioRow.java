package io.scenariolens.matrix;

import java.util.List;

public class ScenarioRow {
    private final String id;
    private final List<StubVariation> stubs;
    private final String expectedOutcome;

    public ScenarioRow(String id, List<StubVariation> stubs, String expectedOutcome) {
        this.id = id;
        this.stubs = stubs;
        this.expectedOutcome = expectedOutcome;
    }

    public String getId() {
        return id;
    }

    public List<StubVariation> getStubs() {
        return stubs;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public StubVariation getStub(String varName) {
        return stubs.stream()
            .filter(s -> s.getCallNode().getVariableName().equals(varName))
            .findFirst()
            .orElse(null);
    }
}
