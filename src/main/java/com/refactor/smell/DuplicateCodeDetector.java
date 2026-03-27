package com.refactor.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;

import java.util.*;

/**
 * Detects duplicate code blocks within a compilation unit.
 * Uses normalized statement-sequence fingerprinting.
 * Flags any method body that shares 3+ consecutive normalized statements with another.
 */
public class DuplicateCodeDetector implements SmellDetector {

    private final int minDuplicateStatements;

    public DuplicateCodeDetector() {
        this(3);
    }

    public DuplicateCodeDetector(int minDuplicateStatements) {
        this.minDuplicateStatements = minDuplicateStatements;
    }

    public int getMinDuplicateStatements() {
        return minDuplicateStatements;
    }

    @Override
    public List<CodeSmell> detect(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

        // Build fingerprint -> list of (className, methodName, line) mappings
        Map<String, List<String>> fingerprintMap = new LinkedHashMap<>();

        for (MethodDeclaration method : methods) {
            if (method.getBody().isEmpty()) continue;
            BlockStmt body = method.getBody().get();
            List<Statement> statements = body.getStatements();

            String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                    .map(ClassOrInterfaceDeclaration::getNameAsString)
                    .orElse("Unknown");
            String methodName = method.getNameAsString();
            int line = method.getBegin().map(p -> p.line).orElse(-1);

            // Sliding window of minDuplicateStatements size
            for (int i = 0; i <= statements.size() - minDuplicateStatements; i++) {
                StringBuilder fp = new StringBuilder();
                for (int j = i; j < i + minDuplicateStatements; j++) {
                    fp.append(normalize(statements.get(j))).append("|");
                }
                String key = fp.toString();
                fingerprintMap.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(className + "#" + methodName + "@" + line);
            }
        }

        Set<String> reported = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : fingerprintMap.entrySet()) {
            // Only flag when the same block appears in at least 2 distinct methods
            long distinctMethods = entry.getValue().stream().distinct().count();
            if (distinctMethods > 1) {
                String locations = String.join(", ", entry.getValue().stream().distinct()
                        .collect(java.util.stream.Collectors.toList()));
                if (!reported.contains(locations)) {
                    reported.add(locations);
                    // Report on first location
                    String first = entry.getValue().get(0);
                    String[] parts = first.split("[#@]");
                    String cls = parts[0];
                    String mth = parts.length > 1 ? parts[1] : "";
                    int ln = parts.length > 2 ? Integer.parseInt(parts[2]) : -1;
                    smells.add(new CodeSmell(
                            SmellType.DUPLICATE_CODE,
                            cls, mth, ln,
                            "Duplicate block found in: " + locations
                    ));
                }
            }
        }

        return smells;
    }

    /**
     * Normalises a statement to its structural type, ignoring literal values.
     */
    private String normalize(Statement stmt) {
        // Use class name as structural fingerprint
        return stmt.getClass().getSimpleName();
    }
}
