package io.scenariolens.gap;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.List;

public class AssertionClassifier {

    public String classify(MethodDeclaration testMethod) {
        List<MethodCallExpr> calls = testMethod.findAll(MethodCallExpr.class);
        boolean hasAssert = false;
        boolean hasStrongAssert = false;

        for (MethodCallExpr call : calls) {
            String name = call.getNameAsString();
            if (name.startsWith("assert") || name.equals("verify") || name.equals("assertThat")) {
                hasAssert = true;
                if (name.equals("assertEquals") || name.equals("assertThrows") || name.equals("assertThat")) {
                    hasStrongAssert = true;
                }
            }
        }

        if (hasStrongAssert) return "STRONG";
        if (hasAssert) return "WEAK";
        return "NONE";
    }
}
