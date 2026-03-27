package com.refactor.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.type.PrimitiveType;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects Primitive Obsession: classes where the majority of fields are
 * primitive or raw String types, suggesting missing domain objects.
 * Triggers when primitiveFieldCount >= threshold (default: 5).
 */
public class PrimitiveObsessionDetector implements SmellDetector {

    private static final Set<String> WRAPPER_TYPES = Set.of(
            "String", "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short"
    );

    private final int threshold;

    public PrimitiveObsessionDetector() {
        this(5);
    }

    public PrimitiveObsessionDetector(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            if (cls.isInterface()) return;

            long primitiveCount = cls.getFields().stream()
                    .filter(this::isPrimitiveOrWrapper)
                    .count();

            if (primitiveCount > threshold) {
                int line = cls.getBegin().map(p -> p.line).orElse(-1);
                smells.add(new CodeSmell(
                        SmellType.PRIMITIVE_OBSESSION,
                        cls.getNameAsString(),
                        null,
                        line,
                        "Class has " + primitiveCount + " primitive/String fields (threshold: " + threshold + ")"
                ));
            }
        });

        return smells;
    }

    private boolean isPrimitiveOrWrapper(FieldDeclaration field) {
        if (field.getCommonType() instanceof PrimitiveType) return true;
        String typeName = field.getCommonType().asString();
        return WRAPPER_TYPES.contains(typeName);
    }
}
