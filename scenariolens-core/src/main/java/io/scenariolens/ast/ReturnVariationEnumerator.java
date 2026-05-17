package io.scenariolens.ast;

import io.scenariolens.matrix.StubVariation;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.declarations.ResolvedEnumDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;

import java.util.ArrayList;
import java.util.List;

public class ReturnVariationEnumerator {

    public List<StubVariation> enumerate(CallNode callNode) {
        List<StubVariation> variations = new ArrayList<>();
        String returnType = callNode.getReturnType();

        if (returnType.equals("void")) {
            variations.add(new StubVariation(callNode, StubVariation.VariationType.COMPLETES_NORMALLY, "completes normally", "Success path"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.THROWS_EXCEPTION, "RuntimeException", "Exception path"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.MUST_BE_CALLED, "MUST_BE_CALLED", "Verification"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.NOT_CALLED, "NOT_CALLED", "Verification"));
        } else if (returnType.equals("boolean") || returnType.equals("java.lang.Boolean")) {
            variations.add(new StubVariation(callNode, StubVariation.VariationType.BOOLEAN_TRUE, "true", "True path"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.BOOLEAN_FALSE, "false", "False path"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.THROWS_EXCEPTION, "RuntimeException", "Exception path"));
        } else {
            // In a full implementation, we'd resolve if this is an enum and iterate constants
            // For now, generate basic VALID/NULL variations for reference types
            
            String shortType = returnType;
            if (shortType.contains(".")) {
                shortType = shortType.substring(shortType.lastIndexOf('.') + 1);
            }
            
            variations.add(new StubVariation(callNode, StubVariation.VariationType.VALID_OBJECT, shortType + "(valid)", "Valid return"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.NULL_RETURN, "null", "Null return"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.THROWS_EXCEPTION, "RuntimeException", "Exception path"));
            variations.add(new StubVariation(callNode, StubVariation.VariationType.NOT_CALLED, "NOT_CALLED", "Not called on this path"));
        }

        return variations;
    }
}
