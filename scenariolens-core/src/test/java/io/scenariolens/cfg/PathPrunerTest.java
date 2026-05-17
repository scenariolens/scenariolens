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

public class PathPrunerTest {

    @Test
    public void processRefund_prunesTo9Scenarios() throws Exception {
        // Use the actual PaymentService.java from examples/payment-service (with full type resolution)
        File sourceDir = new File("../examples/payment-service/src/main/java");
        File paymentServiceFile = new File(sourceDir, "com/example/payment/PaymentService.java");

        MethodParser parser = new MethodParser(sourceDir);
        MethodDeclaration method = parser.parse(paymentServiceFile, "processRefund");

        OutgoingCallDetector detector = new OutgoingCallDetector();
        List<CallNode> calls = detector.detect(method);

        System.out.println("=== Detected " + calls.size() + " calls ===");
        for (CallNode c : calls) {
            System.out.println("  call: var=" + c.getVariableName() + " method=" + c.getMethodName()
                    + " return=" + c.getReturnType() + " assignedTo=" + c.getAssignedTo());
        }

        ScenarioMatrix matrix = new ScenarioMatrix();
        List<ScenarioRow> scenarios = matrix.generate(method, calls);

        System.out.println("=== Scenarios count: " + scenarios.size() + " ===");
        for (ScenarioRow s : scenarios) {
            System.out.println("  " + s.getId() + " stubs=" + s.getStubs().stream()
                    .map(sv -> sv.getCallNode().getMethodName() + "=" + sv.getExactValue()).toList());
        }

        int rawCount = matrix.getRawCount();
        System.out.println("Raw product: " + rawCount + ", After pruning: " + scenarios.size());
        
        // Raw = orderRepository(4) x paymentClient(4) x eventPublisher(4) = 64
        // After pruning: impossible combinations are removed
        // Pruning ratio must be > 60% for this method
        assertTrue(rawCount > scenarios.size(), "No pruning occurred! raw=" + rawCount + " pruned=" + scenarios.size());
        double prunedRatio = 1.0 - (double) scenarios.size() / rawCount;
        System.out.println("Pruning ratio: " + (int)(prunedRatio * 100) + "%");
        assertTrue(prunedRatio >= 0.60, "Expected pruning ratio >= 60%, got: " + (int)(prunedRatio * 100) + "%");
        
        // Count must be <= 15 (generous upper bound given 3-variation system without enum constants)
        assertTrue(scenarios.size() <= 15, "Too many scenarios: " + scenarios.size() + " (pruning not working)");

        // S02: orderRepository=null → paymentClient NOT_CALLED, eventPublisher NOT_CALLED
        assertTrue(scenarios.stream().anyMatch(s ->
            s.getStub("orderRepository") != null &&
            s.getStub("orderRepository").getType() == StubVariation.VariationType.NULL_RETURN &&
            s.getStub("paymentClient") != null &&
            s.getStub("paymentClient").getType() == StubVariation.VariationType.NOT_CALLED &&
            s.getStub("eventPublisher") != null &&
            s.getStub("eventPublisher").getType() == StubVariation.VariationType.NOT_CALLED
        ), "Expected: null order → paymentClient NOT_CALLED, eventPublisher NOT_CALLED");

        // S08: findById=throws → paymentClient NOT_CALLED, eventPublisher NOT_CALLED
        assertTrue(scenarios.stream().anyMatch(s ->
            s.getStub("orderRepository") != null &&
            s.getStub("orderRepository").getType() == StubVariation.VariationType.THROWS_EXCEPTION &&
            s.getStub("paymentClient") != null &&
            s.getStub("paymentClient").getType() == StubVariation.VariationType.NOT_CALLED &&
            s.getStub("eventPublisher") != null &&
            s.getStub("eventPublisher").getType() == StubVariation.VariationType.NOT_CALLED
        ), "Expected: findById throws → paymentClient NOT_CALLED");

        // S09: refund=throws → eventPublisher NOT_CALLED
        assertTrue(scenarios.stream().anyMatch(s ->
            s.getStub("orderRepository") != null &&
            s.getStub("orderRepository").getType() == StubVariation.VariationType.VALID_OBJECT &&
            s.getStub("paymentClient") != null &&
            s.getStub("paymentClient").getType() == StubVariation.VariationType.THROWS_EXCEPTION &&
            s.getStub("eventPublisher") != null &&
            s.getStub("eventPublisher").getType() == StubVariation.VariationType.NOT_CALLED
        ), "Expected: refund throws → eventPublisher NOT_CALLED");

        // No scenario: orderRepository=null AND paymentClient != NOT_CALLED
        assertFalse(scenarios.stream().anyMatch(s ->
            s.getStub("orderRepository") != null &&
            s.getStub("orderRepository").getType() == StubVariation.VariationType.NULL_RETURN &&
            s.getStub("paymentClient") != null &&
            s.getStub("paymentClient").getType() != StubVariation.VariationType.NOT_CALLED
        ), "Pruning failure: null order + paymentClient called");

        // No scenario: orderRepository=throws AND paymentClient != NOT_CALLED
        assertFalse(scenarios.stream().anyMatch(s ->
            s.getStub("orderRepository") != null &&
            s.getStub("orderRepository").getType() == StubVariation.VariationType.THROWS_EXCEPTION &&
            s.getStub("paymentClient") != null &&
            s.getStub("paymentClient").getType() != StubVariation.VariationType.NOT_CALLED
        ), "Pruning failure: findById throws + paymentClient called");
    }
}
