package io.scenariolens.gap;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

public class TestClassParser {
    private final JavaParser javaParser;

    public TestClassParser(JavaParser javaParser) {
        this.javaParser = javaParser;
    }

    public List<MethodDeclaration> parseTests(File testFile) throws FileNotFoundException {
        CompilationUnit cu = javaParser.parse(testFile).getResult().orElseThrow(
            () -> new IllegalArgumentException("Failed to parse " + testFile.getAbsolutePath())
        );
        
        return cu.findAll(MethodDeclaration.class).stream()
                 .filter(m -> m.isAnnotationPresent("Test") || m.isAnnotationPresent("ParameterizedTest"))
                 .collect(Collectors.toList());
    }
}
