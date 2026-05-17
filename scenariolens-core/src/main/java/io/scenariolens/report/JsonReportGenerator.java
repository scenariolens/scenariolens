package io.scenariolens.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

public class JsonReportGenerator {
    private final ObjectMapper mapper;

    public JsonReportGenerator() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void generate(java.util.List<GapReport> reports, File outputDir) {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File file = new File(outputDir, "report.json");
            
            int totalScenarios = reports.stream().mapToInt(GapReport::getTotalScenarios).sum();
            int totalCovered   = reports.stream().mapToInt(GapReport::getCoveredScenarios).sum();
            int totalMissing   = totalScenarios - totalCovered;
            int covPct = totalScenarios == 0 ? 100 : (int)((totalCovered * 100.0) / totalScenarios);

            java.util.LinkedHashMap<String, java.util.List<GapReport>> byClass = new java.util.LinkedHashMap<>();
            for (GapReport r : reports) {
                if (r.getTotalScenarios() <= 1 && r.getMissingScenarios().isEmpty() && r.getCoveredRows().isEmpty()) continue;
                byClass.computeIfAbsent(r.getClassName(), k -> new java.util.ArrayList<>()).add(r);
            }

            long classesWithGaps = byClass.values().stream()
                .filter(ms -> ms.stream().anyMatch(r -> !r.getMissingScenarios().isEmpty()))
                .count();

            java.util.Map<String, Object> summary = new java.util.LinkedHashMap<>();
            summary.put("dscScore", covPct);
            summary.put("totalClasses", byClass.size());
            summary.put("classesWithGaps", classesWithGaps);
            summary.put("totalMethodsAnalyzed", reports.size());
            summary.put("totalScenarios", totalScenarios);
            summary.put("totalCovered", totalCovered);
            summary.put("totalMissing", totalMissing);

            java.util.Map<String, Object> output = new java.util.LinkedHashMap<>();
            output.put("summary", summary);
            output.put("classes", byClass);

            mapper.writeValue(file, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
