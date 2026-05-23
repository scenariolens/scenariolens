package io.scenariolens.cfg;

import io.scenariolens.ast.CallNode;
import io.scenariolens.ast.MethodParser;
import io.scenariolens.ast.OutgoingCallDetector;
import io.scenariolens.matrix.ScenarioMatrix;
import io.scenariolens.matrix.ScenarioRow;
import io.scenariolens.matrix.StubVariation;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EdgeCaseTest {

    private final File sourceDir = new File("../examples/payment-service/src/main/java");
    private final File paymentServiceFile = new File(sourceDir, "com/example/payment/PaymentService.java");
    private final MethodParser parser = new MethodParser(sourceDir);
    private final OutgoingCallDetector detector = new OutgoingCallDetector();
    private final ScenarioMatrix matrix = new ScenarioMatrix();

    @Test
    public void test1_methodWithNoOutgoingCalls() throws Exception {
        MethodDeclaration method = parser.parse(paymentServiceFile, "isValidAmount");
        List<CallNode> calls = detector.detect(method);
        
        assertEquals(0, calls.size());
        
        List<ScenarioRow> scenarios = matrix.generate(method, calls);
        assertEquals(1, scenarios.size());
        
        ScenarioRow scenario = scenarios.get(0);
        assertTrue(scenario.getStubs().isEmpty());
        assertEquals("returns boolean", scenario.getExpectedOutcome());
    }

    @Test
    public void test2_switchStatementEnumMutualExclusivity() throws Exception {
        MethodDeclaration method = parser.parse(paymentServiceFile, "getRefundPolicy");
        List<CallNode> calls = detector.detect(method);
        
        assertEquals(2, calls.size());
        
        List<ScenarioRow> scenarios = matrix.generate(method, calls);
        
        boolean hasPremiumScenario = false;
        boolean hasStandardScenario = false;
        
        for (ScenarioRow s : scenarios) {
            boolean premiumCalled = false;
            boolean standardCalled = false;
            for (StubVariation stub : s.getStubs()) {
                if (stub.getCallNode().getMethodName().equals("getPremiumPolicy")) {
                    premiumCalled = stub.getType() != StubVariation.VariationType.NOT_CALLED;
                }
                if (stub.getCallNode().getMethodName().equals("getStandardPolicy")) {
                    standardCalled = stub.getType() != StubVariation.VariationType.NOT_CALLED;
                }
            }
            
            // Mutually exclusive: they cannot both be called in the same scenario
            assertFalse(premiumCalled && standardCalled, 
                "Switch case branches getPremiumPolicy and getStandardPolicy were both called in: " + s.getId());
                
            if (premiumCalled) hasPremiumScenario = true;
            if (standardCalled) hasStandardScenario = true;
        }
        
        assertTrue(hasPremiumScenario);
        assertTrue(hasStandardScenario);
    }

    @Test
    public void test3_duplicateCallsHaveIndependentVariations() throws Exception {
        MethodDeclaration method = parser.parse(paymentServiceFile, "transfer");
        List<CallNode> calls = detector.detect(method);
        
        assertEquals(3, calls.size());
        
        List<ScenarioRow> scenarios = matrix.generate(method, calls);
        
        // Assert independent stub variations per occurrence
        boolean hasFirstNullSecondValid = scenarios.stream().anyMatch(s ->
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[1]") && stub.getType() == StubVariation.VariationType.NULL_RETURN) &&
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[2]") && stub.getType() == StubVariation.VariationType.VALID_OBJECT)
        );
        
        boolean hasFirstValidSecondNull = scenarios.stream().anyMatch(s ->
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[1]") && stub.getType() == StubVariation.VariationType.VALID_OBJECT) &&
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[2]") && stub.getType() == StubVariation.VariationType.NULL_RETURN)
        );
        
        assertTrue(hasFirstNullSecondValid, "Should have a scenario where call 1 returns null and call 2 returns valid");
        assertTrue(hasFirstValidSecondNull, "Should have a scenario where call 1 returns valid and call 2 returns null");
    }

    @Test
    public void test4_tryCatchGeneratesNormalAndExceptionPaths() throws Exception {
        MethodDeclaration method = parser.parse(paymentServiceFile, "processRefund");
        List<CallNode> calls = detector.detect(method);
        
        List<ScenarioRow> scenarios = matrix.generate(method, calls);
        
        boolean hasNormalPath = scenarios.stream().anyMatch(s ->
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getMethodName().equals("refund") && stub.getType() == StubVariation.VariationType.VALID_OBJECT) &&
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getMethodName().equals("publish") && stub.getType() == StubVariation.VariationType.COMPLETES_NORMALLY)
        );
        
        boolean hasExceptionPath = scenarios.stream().anyMatch(s ->
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getMethodName().equals("refund") && stub.getType() == StubVariation.VariationType.THROWS_EXCEPTION) &&
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getMethodName().equals("publish") && stub.getType() == StubVariation.VariationType.NOT_CALLED)
        );
        
        assertTrue(hasNormalPath, "Should have a scenario for the normal path");
        assertTrue(hasExceptionPath, "Should have a scenario for the exception path routed to the catch block");
    }

    @Test
    public void test5_compoundOrNullCheckGeneratesSeparateNullScenarios() throws Exception {
        MethodDeclaration method = parser.parse(paymentServiceFile, "transfer");
        List<CallNode> calls = detector.detect(method);
        
        List<ScenarioRow> scenarios = matrix.generate(method, calls);
        
        boolean hasSourceNullScenario = scenarios.stream().anyMatch(s ->
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[1]") && stub.getType() == StubVariation.VariationType.NULL_RETURN) &&
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[2]") && stub.getType() == StubVariation.VariationType.VALID_OBJECT)
        );
        
        boolean hasTargetNullScenario = scenarios.stream().anyMatch(s ->
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[1]") && stub.getType() == StubVariation.VariationType.VALID_OBJECT) &&
            s.getStubs().stream().anyMatch(stub -> stub.getCallNode().getUniqueKey().equals("findById[2]") && stub.getType() == StubVariation.VariationType.NULL_RETURN)
        );
        
        assertTrue(hasSourceNullScenario, "Should have separate scenario when source == null");
        assertTrue(hasTargetNullScenario, "Should have separate scenario when target == null");
    }
}
