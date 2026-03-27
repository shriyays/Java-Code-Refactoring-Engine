package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.transform.GodClassRefactorer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targets surviving mutants in GodClassRefactorer:
 * - lambda equality check (line 40: filter by class name)
 * - fieldSplit / methodSplit boundary (line 54)
 * - forEach loops that move members (lines 82-83)
 */
class GodClassRefactorerExtendedTest {

    private final GodClassRefactorer refactorer = new GodClassRefactorer();

    private CodeSmell smell(String cls) {
        return new CodeSmell(SmellType.GOD_CLASS, cls, null, 1, "");
    }

    @Test
    void onlyTargetClassIsRefactoredNotOthers() {
        // Two classes; smell targets only "Target" — "Other" should be unchanged
        String code = "class Target { int a; int b; void m1(){} void m2(){} }"
                + " class Other { int x; void o(){} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        String otherBefore = cu.findAll(
                com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .stream().filter(c -> c.getNameAsString().equals("Other"))
                .findFirst().map(Object::toString).orElse("");

        refactorer.refactor(cu, smell("Target"));

        String otherAfter = cu.findAll(
                com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .stream().filter(c -> c.getNameAsString().equals("Other"))
                .findFirst().map(Object::toString).orElse("");

        assertEquals(otherBefore, otherAfter, "Other class should remain unchanged");
    }

    @Test
    void movedMethodsAreRemovedFromOriginalClass() {
        String code = "class Big {"
                + " int a; int b; int c;"
                + " void p() { int x=1; }"
                + " void q() { int x=2; }"
                + " void r() { int x=3; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        int methodsBefore = cu.findAll(
                com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .stream().filter(c -> c.getNameAsString().equals("Big"))
                .findFirst().map(c -> c.getMethods().size()).orElse(0);

        RefactorResult result = refactorer.refactor(cu, smell("Big"));
        assertTrue(result.isSuccess());

        int methodsAfter = cu.findAll(
                com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .stream().filter(c -> c.getNameAsString().equals("Big"))
                .findFirst().map(c -> c.getMethods().size()).orElse(0);

        assertTrue(methodsAfter < methodsBefore,
                "Some methods should have moved to delegate (" + methodsBefore + " -> " + methodsAfter + ")");
    }

    @Test
    void movedFieldsAreRemovedFromOriginalClass() {
        String code = "class Big {"
                + " int a; int b; int c; int d;"
                + " void p() { int x=1; }"
                + " void q() { int x=2; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        int fieldsBefore = cu.findAll(
                com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .stream().filter(c -> c.getNameAsString().equals("Big"))
                .findFirst().map(c -> c.getFields().size()).orElse(0);

        RefactorResult result = refactorer.refactor(cu, smell("Big"));
        assertTrue(result.isSuccess());

        int fieldsAfter = cu.findAll(
                com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .stream().filter(c -> c.getNameAsString().equals("Big"))
                .findFirst().map(c -> c.getFields().size()).orElse(0);

        // +1 for the delegate field added
        assertTrue(fieldsAfter <= fieldsBefore,
                "Fields should have been redistributed (" + fieldsBefore + " -> " + fieldsAfter + ")");
    }

    @Test
    void classSplitAtMidpoint() {
        // 4 methods → split at 2; delegate should contain 2 methods
        String code = "class G {"
                + " int a; int b;"
                + " void m1(){} void m2(){} void m3(){} void m4(){}"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("G"));
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("2 methods"),
                "Should report 2 moved methods: " + result.getMessage());
    }
}
