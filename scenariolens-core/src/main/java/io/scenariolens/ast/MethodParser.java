package io.scenariolens.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

public class MethodParser {

    private final JavaParser javaParser;

    public MethodParser(File sourceDir) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        if (sourceDir != null && sourceDir.exists()) {
            typeSolver.add(new com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver(sourceDir));
        }
        
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(symbolSolver);
        
        this.javaParser = new JavaParser(parserConfiguration);
    }

    public MethodParser() {
        this((File) null);
    }

    public MethodParser(JavaParser javaParser) {
        this.javaParser = javaParser;
    }

    public MethodDeclaration parse(File sourceFile, String methodName) throws FileNotFoundException {
        Optional<CompilationUnit> cuResult = javaParser.parse(sourceFile).getResult();
        
        if (!cuResult.isPresent()) {
            throw new IllegalArgumentException("Failed to parse file: " + sourceFile.getAbsolutePath());
        }
        
        CompilationUnit cu = cuResult.get();
        Optional<MethodDeclaration> method = cu.findFirst(MethodDeclaration.class, 
                m -> m.getNameAsString().equals(methodName));
                
        if (!method.isPresent()) {
            throw new IllegalArgumentException("Method " + methodName + " not found in file: " + sourceFile.getAbsolutePath());
        }
        
        return method.get();
    }
    
    public List<MethodDeclaration> parseAll(File sourceFile) throws FileNotFoundException {
        Optional<CompilationUnit> cuResult = javaParser.parse(sourceFile).getResult();
        if (!cuResult.isPresent()) {
            throw new IllegalArgumentException("Failed to parse file: " + sourceFile.getAbsolutePath());
        }
        return cuResult.get().findAll(MethodDeclaration.class);
    }
}
