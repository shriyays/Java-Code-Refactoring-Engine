package com.refactor.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects classes with too many fields or methods (God-class precursor).
 * Default: more than 10 fields OR more than 20 methods.
 */
public class LargeClassDetector implements SmellDetector {

    private final int fieldThreshold;
    private final int methodThreshold;

    public LargeClassDetector() {
        this(10, 20);
    }

    public LargeClassDetector(int fieldThreshold, int methodThreshold) {
        this.fieldThreshold = fieldThreshold;
        this.methodThreshold = methodThreshold;
    }

    public int getFieldThreshold()  { return fieldThreshold; }
    public int getMethodThreshold() { return methodThreshold; }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            if (cls.isInterface()) return;

            int fields  = cls.getFields().size();
            int methods = cls.getMethods().size();

            if (fields > fieldThreshold) {
                int line = cls.getBegin().map(p -> p.line).orElse(-1);
                smells.add(new CodeSmell(
                        SmellType.LARGE_CLASS,
                        cls.getNameAsString(),
                        null,
                        line,
                        "Class has " + fields + " fields (threshold: " + fieldThreshold + ")"
                ));
            }

            if (methods > methodThreshold) {
                int line = cls.getBegin().map(p -> p.line).orElse(-1);
                smells.add(new CodeSmell(
                        SmellType.LARGE_CLASS,
                        cls.getNameAsString(),
                        null,
                        line,
                        "Class has " + methods + " methods (threshold: " + methodThreshold + ")"
                ));
            }
        });

        return smells;
    }
}
