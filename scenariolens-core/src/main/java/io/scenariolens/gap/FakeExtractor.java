package io.scenariolens.gap;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FakeExtractor {
    private final JavaParser javaParser;

    public FakeExtractor(JavaParser javaParser) {
        this.javaParser = javaParser;
    }

    public Map<String, Map<String, List<String>>> extractFakes(File testSourceDirectory) {
        Map<String, Map<String, List<String>>> globalFakes = new HashMap<>();
        
        if (testSourceDirectory == null || !testSourceDirectory.exists()) return globalFakes;

        try (Stream<Path> walk = Files.walk(testSourceDirectory.toPath())) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .map(Path::toFile)
                .forEach(file -> processFile(file, globalFakes));
        } catch (Exception e) {
            // Ignore file traversal issues
        }
        
        return globalFakes;
    }

    private void processFile(File file, Map<String, Map<String, List<String>>> globalFakes) {
        try {
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) return;
            
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                if (!clazz.isInterface() && clazz.getImplementedTypes().isNonEmpty()) {
                    clazz.getImplementedTypes().forEach(implementedType -> {
                        String interfaceName = implementedType.getNameAsString();
                        Map<String, List<String>> fakeMethods = new HashMap<>();
                        
                        clazz.getMethods().forEach(method -> {
                            List<String> stubValues = extractStubValues(method);
                            if (!stubValues.isEmpty()) {
                                fakeMethods.put(method.getNameAsString(), stubValues);
                            }
                        });
                        
                        if (!fakeMethods.isEmpty()) {
                            globalFakes.put(interfaceName, fakeMethods);
                        }
                    });
                }
            });
        } catch (Exception e) {
            // Ignore parse errors for individual files
        }
    }

    private List<String> extractStubValues(MethodDeclaration method) {
        List<String> results = new java.util.ArrayList<>();
        // Look for throwing an exception
        List<ThrowStmt> throwsStmts = method.findAll(ThrowStmt.class);
        if (!throwsStmts.isEmpty()) {
            results.add("throws RuntimeException"); // Simplified MVP assumption
        }
        
        // Look for return statements
        List<ReturnStmt> returns = method.findAll(ReturnStmt.class);
        for (ReturnStmt returnStmt : returns) {
            if (returnStmt.getExpression().isPresent()) {
                String exprStr = returnStmt.getExpression().get().toString();
                if (exprStr.equals("null") || exprStr.equals("true") || exprStr.equals("false")) {
                    if (!results.contains(exprStr)) results.add(exprStr);
                } else {
                    String returnType = method.getType().asString();
                    if (returnType.contains(".")) {
                        returnType = returnType.substring(returnType.lastIndexOf('.') + 1);
                    }
                    String val;
                    if (returnType.startsWith("Optional<")) {
                        val = exprStr.contains("empty()") ? "Optional.empty()" : "Optional(valid)";
                    } else {
                        val = returnType + "(valid)";
                    }
                    if (!results.contains(val)) results.add(val);
                }
            }
        }
        return results;
    }
}
