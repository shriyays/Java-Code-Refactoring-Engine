package com.refactor.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects God Classes — classes that violate the Single Responsibility Principle
 * by combining high field count, high method count, and high total LOC.
 *
 * Triggers when ALL three of the following are true:
 *   - fields  >= fieldThreshold
 *   - methods >= methodThreshold
 *   - total method statement count >= locThreshold
 */
public class GodClassDetector implements SmellDetector {

    private final int fieldThreshold;
    private final int methodThreshold;
    private final int locThreshold;

    public GodClassDetector() {
        this(7, 15, 80);
    }

    public GodClassDetector(int fieldThreshold, int methodThreshold, int locThreshold) {
        this.fieldThreshold  = fieldThreshold;
        this.methodThreshold = methodThreshold;
        this.locThreshold    = locThreshold;
    }

    public int getFieldThreshold()  { return fieldThreshold; }
    public int getMethodThreshold() { return methodThreshold; }
    public int getLocThreshold()    { return locThreshold; }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            if (cls.isInterface()) return;

            int fields  = cls.getFields().size();
            int methods = cls.getMethods().size();
            int loc     = totalStatements(cls);

            if (fields >= fieldThreshold && methods >= methodThreshold && loc >= locThreshold) {
                int line = cls.getBegin().map(p -> p.line).orElse(-1);
                smells.add(new CodeSmell(
                        SmellType.GOD_CLASS,
                        cls.getNameAsString(),
                        null,
                        line,
                        String.format("God class: %d fields, %d methods, %d total statements", fields, methods, loc)
                ));
            }
        });

        return smells;
    }

    private int totalStatements(ClassOrInterfaceDeclaration cls) {
        return cls.getMethods().stream()
                .mapToInt(m -> m.getBody().map(b -> b.getStatements().size()).orElse(0))
                .sum();
    }
}
