package io.scenariolens.gap;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockitoStubExtractor {

    public Map<String, String> extractStubs(MethodDeclaration testMethod) {
        Map<String, String> stubs = new HashMap<>();

        List<MethodCallExpr> calls = testMethod.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : calls) {
            String methodName = call.getNameAsString();
            if (methodName.equals("thenReturn") || methodName.equals("thenThrow")) {
                if (call.getScope().isPresent() && call.getScope().get().isMethodCallExpr()) {
                    MethodCallExpr whenCall = call.getScope().get().asMethodCallExpr();
                    if (whenCall.getNameAsString().equals("when") && !whenCall.getArguments().isEmpty()) {
                        if (whenCall.getArgument(0).isMethodCallExpr()) {
                            MethodCallExpr stubbedMethod = whenCall.getArgument(0).asMethodCallExpr();
                            String stubbedName = stubbedMethod.getNameAsString();
                            String returnValue = call.getArgument(0).toString();
                            
                            stubs.put(stubbedName, methodName.equals("thenThrow") ? "throws RuntimeException" : returnValue);
                        }
                    }
                }
            }
        }

        return stubs;
    }
}
