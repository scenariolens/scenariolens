package io.scenariolens.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LcovReportGenerator {

    public void generate(List<GapReport> reports, File outputDirectory) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        File file = new File(outputDirectory, "scenariolens.lcov");
        try (FileWriter writer = new FileWriter(file)) {
            // Group reports by file path
            Map<String, List<GapReport>> reportsByFile = reports.stream()
                    .collect(Collectors.groupingBy(GapReport::getFilePath));

            for (Map.Entry<String, List<GapReport>> entry : reportsByFile.entrySet()) {
                String filePath = entry.getKey();
                List<GapReport> fileReports = entry.getValue();

                writer.write("TN:\n");
                writer.write("SF:" + filePath + "\n");

                int fnf = 0, fnh = 0;
                int brf = 0, brh = 0;
                int lf = 0, lh = 0;

                // Function Definitions
                for (GapReport report : fileReports) {
                    writer.write("FN:" + report.getLineNumber() + "," + report.getMethodName() + "\n");
                }

                // Function Execution Counts
                for (GapReport report : fileReports) {
                    int hit = report.getCoveredScenarios() > 0 ? 1 : 0;
                    writer.write("FNDA:" + hit + "," + report.getMethodName() + "\n");
                    fnf++;
                    if (hit > 0) fnh++;
                }

                writer.write("FNF:" + fnf + "\n");
                writer.write("FNH:" + fnh + "\n");

                // Branch Data
                for (GapReport report : fileReports) {
                    int line = report.getLineNumber();
                    int total = report.getTotalScenarios();
                    int covered = report.getCoveredScenarios();

                    if (total > 0) {
                        for (int i = 0; i < total; i++) {
                            int taken = (i < covered) ? 1 : 0;
                            writer.write("BRDA:" + line + ",0," + i + "," + taken + "\n");
                        }
                        brf += total;
                        brh += covered;
                    }
                }

                writer.write("BRF:" + brf + "\n");
                writer.write("BRH:" + brh + "\n");

                // Line Data
                for (GapReport report : fileReports) {
                    int hit = report.getCoveredScenarios() > 0 ? 1 : 0;
                    writer.write("DA:" + report.getLineNumber() + "," + hit + "\n");
                    lf++;
                    if (hit > 0) lh++;
                }

                writer.write("LF:" + lf + "\n");
                writer.write("LH:" + lh + "\n");
                writer.write("end_of_record\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
