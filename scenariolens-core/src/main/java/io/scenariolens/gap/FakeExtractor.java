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

    public Map<String, Map<String, String>> extractFakes(File testSourceDirectory) {
        Map<String, Map<String, String>> globalFakes = new HashMap<>();
        
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

    private void processFile(File file, Map<String, Map<String, String>> globalFakes) {
        try {
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) return;
            
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                if (!clazz.isInterface() && clazz.getImplementedTypes().isNonEmpty()) {
                    clazz.getImplementedTypes().forEach(implementedType -> {
                        String interfaceName = implementedType.getNameAsString();
                        Map<String, String> fakeMethods = new HashMap<>();
                        
                        clazz.getMethods().forEach(method -> {
                            String stubValue = extractStubValue(method);
                            if (stubValue != null) {
                                fakeMethods.put(method.getNameAsString(), stubValue);
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

    private String extractStubValue(MethodDeclaration method) {
        // Look for throwing an exception
        List<ThrowStmt> throwsStmts = method.findAll(ThrowStmt.class);
        if (!throwsStmts.isEmpty()) {
            return "throws RuntimeException"; // Simplified MVP assumption
        }
        
        // Look for a return statement
        List<ReturnStmt> returns = method.findAll(ReturnStmt.class);
        if (!returns.isEmpty()) {
            ReturnStmt returnStmt = returns.get(0);
            if (returnStmt.getExpression().isPresent()) {
                String exprStr = returnStmt.getExpression().get().toString();
                if (exprStr.equals("null") || exprStr.equals("true") || exprStr.equals("false")) {
                    return exprStr;
                } else {
                    String returnType = method.getType().asString();
                    if (returnType.contains(".")) {
                        returnType = returnType.substring(returnType.lastIndexOf('.') + 1);
                    }
                    if (returnType.startsWith("Optional<")) {
                        return exprStr.contains("empty()") ? "Optional.empty()" : "Optional(valid)";
                    }
                    return returnType + "(valid)";
                }
            }
        }
        return null; // Void or no clear return
    }
}
