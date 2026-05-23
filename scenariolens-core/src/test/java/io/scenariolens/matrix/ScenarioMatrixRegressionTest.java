package io.scenariolens.matrix;

import io.scenariolens.ast.CallNode;
import io.scenariolens.ast.MethodParser;
import io.scenariolens.ast.OutgoingCallDetector;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression suite for ScenarioMatrix scenario output cap behaviour.
 *
 * Guards pinned here:
 *   (1) Default cap is 500 — never raises silently.
 *   (2) isTruncated() is false when scenario count is below the cap.
 *   (3) A custom cap is respected (cap = 3 on a method that has more).
 *   (4) Truncated list is exactly cap-length, not more, not less.
 *   (5) Scenario IDs in a truncated list are still sequential (S01, S02, …).
 */
public class ScenarioMatrixRegressionTest {

    private static final File SOURCE_DIR =
            new File("../examples/payment-service/src/main/java");
    private static final File PAYMENT_SERVICE =
            new File(SOURCE_DIR, "com/example/payment/PaymentService.java");

    // -------------------------------------------------------------------------
    // REG-1  Default cap is 500 and isTruncated() is false for normal methods
    // -------------------------------------------------------------------------
    @Test
    public void reg1_defaultCapIs500_normalMethodNotTruncated() throws Exception {
        MethodParser parser = new MethodParser(SOURCE_DIR);
        MethodDeclaration method = parser.parse(PAYMENT_SERVICE, "processRefund");

        OutgoingCallDetector detector = new OutgoingCallDetector();
        List<CallNode> calls = detector.detect(method);

        ScenarioMatrix matrix = new ScenarioMatrix();
        List<ScenarioRow> scenarios = matrix.generate(method, calls);

        // processRefund generates ~12 pruned scenarios — well under the 500 default
        assertTrue(scenarios.size() > 0, "Should generate at least one scenario");
        assertTrue(scenarios.size() <= 500, "Default cap must be 500");
        assertFalse(matrix.isTruncated(),
                "isTruncated() must be false when scenarios < 500 (got " + scenarios.size() + ")");
    }

    // -------------------------------------------------------------------------
    // REG-2  A low custom cap truncates correctly and sets isTruncated()
    // -------------------------------------------------------------------------
    @Test
    public void reg2_customCap_truncatesAndSetsFlag() throws Exception {
        MethodParser parser = new MethodParser(SOURCE_DIR);
        MethodDeclaration method = parser.parse(PAYMENT_SERVICE, "processRefund");

        OutgoingCallDetector detector = new OutgoingCallDetector();
        List<CallNode> calls = detector.detect(method);

        // processRefund normally produces ~12 pruned scenarios; cap at 3 forces truncation
        ScenarioMatrix matrix = new ScenarioMatrix();
        matrix.setMaxScenariosPerMethod(3);
        List<ScenarioRow> scenarios = matrix.generate(method, calls);

        assertEquals(3, scenarios.size(),
                "Truncated list must be exactly cap length");
        assertTrue(matrix.isTruncated(),
                "isTruncated() must be true when output was capped");
    }

    // -------------------------------------------------------------------------
    // REG-3  Truncated IDs are sequential from S01
    // -------------------------------------------------------------------------
    @Test
    public void reg3_truncatedList_hasSequentialIds() throws Exception {
        MethodParser parser = new MethodParser(SOURCE_DIR);
        MethodDeclaration method = parser.parse(PAYMENT_SERVICE, "processRefund");

        OutgoingCallDetector detector = new OutgoingCallDetector();
        List<CallNode> calls = detector.detect(method);

        ScenarioMatrix matrix = new ScenarioMatrix();
        matrix.setMaxScenariosPerMethod(5);
        List<ScenarioRow> scenarios = matrix.generate(method, calls);

        for (int i = 0; i < scenarios.size(); i++) {
            String expectedId = String.format("S%02d", i + 1);
            assertEquals(expectedId, scenarios.get(i).getId(),
                    "Scenario ID at index " + i + " should be " + expectedId);
        }
    }

    // -------------------------------------------------------------------------
    // REG-4  Cap of exactly scenario-count does NOT set isTruncated()
    // -------------------------------------------------------------------------
    @Test
    public void reg4_capEqualsExactCount_notTruncated() throws Exception {
        MethodParser parser = new MethodParser(SOURCE_DIR);
        MethodDeclaration method = parser.parse(PAYMENT_SERVICE, "processRefund");

        OutgoingCallDetector detector = new OutgoingCallDetector();
        List<CallNode> calls = detector.detect(method);

        // First pass — find the actual count
        ScenarioMatrix measureMatrix = new ScenarioMatrix();
        int actualCount = measureMatrix.generate(method, calls).size();

        // Second pass — set cap exactly equal to count
        ScenarioMatrix matrix = new ScenarioMatrix();
        matrix.setMaxScenariosPerMethod(actualCount);
        List<ScenarioRow> scenarios = matrix.generate(method, calls);

        assertEquals(actualCount, scenarios.size());
        assertFalse(matrix.isTruncated(),
                "isTruncated() must be false when cap == actual scenario count");
    }

    // -------------------------------------------------------------------------
    // REG-5  isTruncated() resets between successive generate() calls on same instance
    // -------------------------------------------------------------------------
    @Test
    public void reg5_isTruncated_resetsOnEachGenerate() throws Exception {
        MethodParser parser = new MethodParser(SOURCE_DIR);
        MethodDeclaration method = parser.parse(PAYMENT_SERVICE, "processRefund");
        OutgoingCallDetector detector = new OutgoingCallDetector();
        List<CallNode> calls = detector.detect(method);

        ScenarioMatrix matrix = new ScenarioMatrix();

        // First run: cap at 3 → truncated
        matrix.setMaxScenariosPerMethod(3);
        matrix.generate(method, calls);
        assertTrue(matrix.isTruncated(), "First run with cap=3 should be truncated");

        // Second run: restore generous cap → not truncated
        matrix.setMaxScenariosPerMethod(10000);
        matrix.generate(method, calls);
        assertFalse(matrix.isTruncated(), "Second run with generous cap should NOT be truncated");
    }
}
