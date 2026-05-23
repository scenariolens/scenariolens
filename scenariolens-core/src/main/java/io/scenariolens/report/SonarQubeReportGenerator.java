package io.scenariolens.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SonarQubeReportGenerator {

    public void generate(List<GapReport> reports, File outputDirectory) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File file = new File(outputDirectory, "sonar-coverage.xml");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<coverage version=\"1\">\n");

            // Group reports by file path
            Map<String, List<GapReport>> reportsByFile = reports.stream()
                    .collect(Collectors.groupingBy(GapReport::getFilePath));

            for (Map.Entry<String, List<GapReport>> entry : reportsByFile.entrySet()) {
                String filePath = entry.getKey();
                writer.write("  <file path=\"" + filePath + "\">\n");

                for (GapReport report : entry.getValue()) {
                    int lineNumber = report.getLineNumber();
                    int total = report.getTotalScenarios();
                    int coveredCount = report.getCoveredScenarios();
                    
                    // If coveredCount > 0, the method was at least partially exercised.
                    boolean covered = coveredCount > 0;

                    if (total > 0) {
                        writer.write("    <lineToCover lineNumber=\"" + lineNumber 
                            + "\" covered=\"" + covered 
                            + "\" branchesToCover=\"" + total 
                            + "\" coveredBranches=\"" + coveredCount + "\"/>\n");
                    } else {
                        writer.write("    <lineToCover lineNumber=\"" + lineNumber 
                            + "\" covered=\"" + covered + "\"/>\n");
                    }
                }

                writer.write("  </file>\n");
            }

            writer.write("</coverage>\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
