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
            mapper.writeValue(file, reports);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
