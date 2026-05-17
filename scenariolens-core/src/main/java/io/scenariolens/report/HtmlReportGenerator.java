package io.scenariolens.report;

import io.scenariolens.matrix.ScenarioRow;
import io.scenariolens.matrix.StubVariation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

public class HtmlReportGenerator {

    public void generate(GapReport report, File outputDir) {
        if (!outputDir.exists()) outputDir.mkdirs();
        File file = new File(outputDir, "report.html");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<html><head><title>ScenarioLens Report</title>");
            writer.write("<style>body { font-family: Arial; } table { border-collapse: collapse; width: 100%; } th, td { border: 1px solid #ddd; padding: 8px; } th { background-color: #f2f2f2; } .missing { color: red; } .covered { color: green; }</style>");
            writer.write("</head><body>");
            
            writer.write("<h1>ScenarioLens Analysis Report</h1>");
            writer.write("<p>Total Scenarios: " + report.getTotalScenarios() + "</p>");
            writer.write("<p>Covered Scenarios: " + report.getCoveredScenarios() + "</p>");
            writer.write("<p>Coverage: " + report.getScenarioCoveragePercent() + "%</p>");
            writer.write("<p>Assertion Strength: " + report.getAssertionStrengthPercent() + "%</p>");
            
            writer.write("<h2>Scenario Matrix</h2>");
            writer.write("<table><tr><th>ID</th><th>Stubs</th><th>Expected Outcome</th><th>Status</th></tr>");
            
            for (ScenarioRow row : report.getCoveredRows()) {
                writeRow(writer, row, "COVERED", "covered");
            }
            for (ScenarioRow row : report.getMissingScenarios()) {
                writeRow(writer, row, "MISSING", "missing");
            }
            
            writer.write("</table>");
            writer.write("</body></html>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeRow(FileWriter writer, ScenarioRow row, String statusText, String cssClass) throws IOException {
        String stubs = row.getStubs().stream()
                .map(s -> s.getCallNode().getVariableName() + "." + s.getCallNode().getMethodName() + " \u2192 " + s.getExactValue())
                .collect(Collectors.joining("<br>"));
                
        writer.write("<tr>");
        writer.write("<td>" + row.getId() + "</td>");
        writer.write("<td>" + stubs + "</td>");
        writer.write("<td>" + row.getExpectedOutcome() + "</td>");
        writer.write("<td class='" + cssClass + "'><strong>" + statusText + "</strong></td>");
        writer.write("</tr>");
    }
}
