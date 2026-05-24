package io.scenariolens.gap;

import com.github.javaparser.JavaParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeExtractorTest {

    @Test
    void extractFakesFromTempDir(@TempDir File tempDir) throws Exception {
        File fakeFile = new File(tempDir, "PaymentClientFake.java");
        Files.writeString(fakeFile.toPath(), 
            "package com.example;\n" +
            "public class PaymentClientFake implements PaymentClient {\n" +
            "    @Override\n" +
            "    public PaymentResponse transfer(int id) {\n" +
            "        if (id == 1) {\n" +
            "             return new PaymentResponse();\n" +
            "        } else {\n" +
            "             throw new RuntimeException(\"fail\");\n" +
            "        }\n" +
            "    }\n" +
            "    @Override\n" +
            "    public boolean ping() {\n" +
            "        return true;\n" +
            "    }\n" +
            "    @Override\n" +
            "    public Object fetchNull() {\n" +
            "        return null;\n" +
            "    }\n" +
            "}"
        );

        FakeExtractor extractor = new FakeExtractor(new JavaParser());
        Map<String, Map<String, List<String>>> fakes = extractor.extractFakes(tempDir);

        assertTrue(fakes.containsKey("PaymentClient"));
        Map<String, List<String>> methods = fakes.get("PaymentClient");
        
        List<String> transferStates = methods.get("transfer");
        assertEquals(2, transferStates.size());
        assertTrue(transferStates.contains("PaymentResponse(valid)"));
        assertTrue(transferStates.contains("throws RuntimeException"));
        
        assertEquals("true", methods.get("ping").get(0));
        assertEquals("null", methods.get("fetchNull").get(0));
    }
}
