package com.refactor.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects methods with more parameters than the configured threshold.
 * Default threshold: 4 parameters.
 */
public class LongParameterListDetector implements SmellDetector {

    private final int threshold;

    public LongParameterListDetector() {
        this(4);
    }

    public LongParameterListDetector(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            int paramCount = method.getParameters().size();
            if (paramCount > threshold) {
                String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::getNameAsString)
                        .orElse("Unknown");
                int line = method.getBegin().map(p -> p.line).orElse(-1);
                smells.add(new CodeSmell(
                        SmellType.LONG_PARAMETER_LIST,
                        className,
                        method.getNameAsString(),
                        line,
                        "Method has " + paramCount + " parameters (threshold: " + threshold + ")"
                ));
            }
        });

        return smells;
    }
}
