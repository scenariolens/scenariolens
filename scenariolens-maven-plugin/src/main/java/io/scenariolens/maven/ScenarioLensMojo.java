package io.scenariolens.maven;

import io.scenariolens.ast.CallNode;
import io.scenariolens.ast.MethodParser;
import io.scenariolens.ast.OutgoingCallDetector;
import io.scenariolens.gap.GapAnalyzer;
import io.scenariolens.gap.TestClassParser;
import io.scenariolens.matrix.ScenarioMatrix;
import io.scenariolens.matrix.ScenarioRow;
import io.scenariolens.report.GapReport;
import io.scenariolens.report.HtmlReportGenerator;
import io.scenariolens.report.JsonReportGenerator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

@Mojo(name = "analyze", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST)
public class ScenarioLensMojo extends AbstractMojo {

    @Parameter(property = "targetPackage")
    private String targetPackage;

    @Parameter(property = "minScenarioCoverage", defaultValue = "70")
    private int minScenarioCoverage;

    @Parameter(property = "failOnMissing", defaultValue = "true")
    private boolean failOnMissing;

    @Parameter(property = "failOnBoundary", defaultValue = "false")
    private boolean failOnBoundary;

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/scenariolens")
    private File outputDirectory;

    @Parameter(property = "formats")
    private List<String> formats;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", required = true, readonly = true)
    private File sourceDirectory;

    @Parameter(defaultValue = "${project.build.testSourceDirectory}", required = true, readonly = true)
    private File testSourceDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("ScenarioLens Phase 1 Analysis starting...");
        
        try {
            String pkg = targetPackage != null ? targetPackage : "com.example.payment";
            String pkgPath = pkg.replace('.', '/');
            File packageDir = new File(sourceDirectory, pkgPath);
            File testPackageDir = new File(testSourceDirectory, pkgPath);
            
            if (!packageDir.exists()) {
                getLog().warn("Could not find package " + pkg + ". Skipping analysis.");
                return;
            }

            List<GapReport> allReports = new ArrayList<>();
            TestClassParser testParser = new TestClassParser(new JavaParser());

            long totalProcessingTime = 0;
            int classesAnalyzed = 0;
            int totalMethods = 0;
            int totalScenarios = 0;

            List<File> javaFiles = new java.util.ArrayList<>();
            try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(packageDir.toPath())) {
                walk.filter(p -> p.toString().endsWith(".java"))
                    .map(java.nio.file.Path::toFile)
                    .forEach(javaFiles::add);
            }
            
            for (File file : javaFiles) {
                classesAnalyzed++;
                // Mirror the relative path in the test directory (handles subpackages)
                java.nio.file.Path relativePath = packageDir.toPath().relativize(file.toPath());
                String testFileName = file.getName().replace(".java", "Test.java");
                File testFile = testPackageDir.toPath().resolve(relativePath).getParent().resolve(testFileName).toFile();
                
                MethodParser parser = new MethodParser(sourceDirectory);
                List<MethodDeclaration> methods = parser.parseAll(file);

                List<MethodDeclaration> tests = new ArrayList<>();
                if (testFile.exists()) {
                    tests = testParser.parseTests(testFile);
                }

                for (MethodDeclaration method : methods) {
                    if (method.isConstructorDeclaration() || method.isPrivate() || method.getNameAsString().equals(file.getName().replace(".java", ""))) continue;
                    
                    totalMethods++;
                    long startTime = System.currentTimeMillis();
                    getLog().info("========================================");
                    getLog().info("Analyzing method: " + method.getNameAsString());
                    
                    OutgoingCallDetector detector = new OutgoingCallDetector();
                    List<CallNode> calls = detector.detect(method);
                    getLog().info("Outgoing calls detected: " + calls.size());

                    ScenarioMatrix matrixGen = new ScenarioMatrix();
                    List<ScenarioRow> matrix = matrixGen.generate(method, calls);
                    int rawCount = matrixGen.getRawCount();
                    int prunedCount = matrix.size();
                    totalScenarios += prunedCount;
                    int ratio = rawCount == 0 ? 0 : (100 - (prunedCount * 100 / rawCount));
                    getLog().info("  raw: " + rawCount + " | pruned: " + (rawCount - prunedCount) + " | final: " + prunedCount + " | pruning ratio: " + ratio + "%");
                    
                    GapAnalyzer analyzer = new GapAnalyzer();
                    GapReport report = analyzer.analyze(method.getNameAsString(), matrix, tests);
                    // Add some hacky fields to Json string or just use the generator
                    allReports.add(report);
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    totalProcessingTime += processingTime;
                }
            }
            
            getLog().info("Classes analyzed: " + classesAnalyzed);
            getLog().info("Total methods: " + totalMethods);
            getLog().info("Total scenarios: " + totalScenarios);
            getLog().info("Processing time: " + totalProcessingTime + "ms");
            getLog().info("Crash: N");

            if (!allReports.isEmpty()) {
                HtmlReportGenerator htmlGen = new HtmlReportGenerator();
                htmlGen.generateAll(allReports, outputDirectory);

                JsonReportGenerator jsonGen = new JsonReportGenerator();
                jsonGen.generate(allReports, outputDirectory);
            }
            
        } catch (Exception e) {
            getLog().error("Tool crashed yes or no: yes", e);
            throw new MojoExecutionException("Error during analysis", e);
        }
    }
}
