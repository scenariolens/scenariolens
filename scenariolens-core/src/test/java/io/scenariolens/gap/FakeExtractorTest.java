package io.scenariolens.gap;

import com.github.javaparser.JavaParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
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
            "    public PaymentResponse transfer() {\n" +
            "        return new PaymentResponse();\n" +
            "    }\n" +
            "    @Override\n" +
            "    public boolean ping() {\n" +
            "        return true;\n" +
            "    }\n" +
            "    @Override\n" +
            "    public Object fetchNull() {\n" +
            "        return null;\n" +
            "    }\n" +
            "    @Override\n" +
            "    public void doThrow() {\n" +
            "        throw new RuntimeException(\"fail\");\n" +
            "    }\n" +
            "}"
        );

        FakeExtractor extractor = new FakeExtractor(new JavaParser());
        Map<String, Map<String, String>> fakes = extractor.extractFakes(tempDir);

        assertTrue(fakes.containsKey("PaymentClient"));
        Map<String, String> methods = fakes.get("PaymentClient");
        
        assertEquals("PaymentResponse(valid)", methods.get("transfer"));
        assertEquals("true", methods.get("ping"));
        assertEquals("null", methods.get("fetchNull"));
        assertEquals("throws RuntimeException", methods.get("doThrow"));
    }
}
