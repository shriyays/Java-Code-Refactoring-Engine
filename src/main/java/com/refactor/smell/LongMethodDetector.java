package com.refactor.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects methods whose statement count exceeds the configured threshold.
 * Default threshold: 20 statements.
 */
public class LongMethodDetector implements SmellDetector {

    private final int threshold;

    public LongMethodDetector() {
        this(20);
    }

    public LongMethodDetector(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            if (method.getBody().isEmpty()) return;

            int statementCount = countStatements(method);
            if (statementCount > threshold) {
                String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(c -> c.getNameAsString())
                        .orElse("Unknown");
                int line = method.getBegin().map(p -> p.line).orElse(-1);
                smells.add(new CodeSmell(
                        SmellType.LONG_METHOD,
                        className,
                        method.getNameAsString(),
                        line,
                        "Method has " + statementCount + " statements (threshold: " + threshold + ")"
                ));
            }
        });

        return smells;
    }

    private int countStatements(MethodDeclaration method) {
        return method.getBody()
                .map(body -> body.getStatements().size())
                .orElse(0);
    }
}
