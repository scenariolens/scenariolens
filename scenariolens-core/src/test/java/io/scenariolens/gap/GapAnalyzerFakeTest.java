package io.scenariolens.gap;

import io.scenariolens.matrix.ScenarioRow;
import io.scenariolens.matrix.StubVariation;
import io.scenariolens.ast.CallNode;
import io.scenariolens.report.GapReport;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GapAnalyzerFakeTest {

    @Test
    void testHeuristicFakeBinding() {
        Map<String, Map<String, List<String>>> globalFakes = new HashMap<>();
        Map<String, List<String>> paymentClientFakes = new HashMap<>();
        paymentClientFakes.put("transfer", Arrays.asList("PaymentResponse(valid)", "throws RuntimeException"));
        globalFakes.put("PaymentClient", paymentClientFakes);

        CallNode call = new CallNode(null, "paymentClient", "transfer", "PaymentClient", "PaymentResponse", null);
        
        ScenarioRow s1 = new ScenarioRow("S01", Collections.singletonList(
            new StubVariation(call, StubVariation.VariationType.VALID_OBJECT, "PaymentResponse(valid)", "Success")
        ), "process");
        ScenarioRow s2 = new ScenarioRow("S02", Collections.singletonList(
            new StubVariation(call, StubVariation.VariationType.THROWS_EXCEPTION, "throws RuntimeException", "Exception")
        ), "process");
        List<ScenarioRow> matrix = Arrays.asList(s1, s2);

        JavaParser parser = new JavaParser();
        String testCode = "class TestClass {\n" +
                          "    void testSuccess() { assertEquals(1, 1); }\n" +
                          "    void testException() { assertThrows(RuntimeException.class, () -> {}); }\n" +
                          "}";
        CompilationUnit cu = parser.parse(testCode).getResult().get();
        List<MethodDeclaration> tests = cu.findAll(MethodDeclaration.class);

        GapAnalyzer analyzer = new GapAnalyzer();
        GapReport report = analyzer.analyze("com.example.PaymentService", "process", "PaymentService.java", 10, matrix, tests, globalFakes);

        assertEquals(0, report.getMissingScenarios().size(), "Both scenarios should be covered using heuristic binding");
        assertEquals(2, report.getCoveredRows().size(), "Two scenarios must be covered");
        assertTrue(report.isUsedHeuristicMapping(), "Heuristic mapping flag must be set to true");
    }
}
