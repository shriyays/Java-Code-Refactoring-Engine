package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.SmellType;
import com.refactor.smell.DuplicateCodeDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targets surviving mutants in DuplicateCodeDetector:
 * - sliding-window boundary (i <= size - min vs i < size - min)
 * - window size arithmetic
 * - entry.getValue().size() > 1 comparison
 */
class DuplicateCodeDetectorExtendedTest {

    @Test
    void slidingWindowDetectsBlockAtStartOfMethod() {
        // Window of 2 — first two statements match between m1 and m2
        DuplicateCodeDetector d = new DuplicateCodeDetector(2);
        String code = "class C {"
                + " void m1() { int a=1; int b=2; int c=99; }"
                + " void m2() { int a=1; int b=2; int c=100; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "Duplicate window at start should be found");
    }

    @Test
    void slidingWindowDetectsBlockAtEndOfMethod() {
        // Window of 2 — last two statements match
        DuplicateCodeDetector d = new DuplicateCodeDetector(2);
        String code = "class C {"
                + " void m1() { int x=99; int a=1; int b=2; }"
                + " void m2() { int x=100; int a=1; int b=2; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "Duplicate window at end should be found");
    }

    @Test
    void exactlyMinStatementsMatchIsDetected() {
        // Both methods have exactly minDuplicateStatements statements that match
        DuplicateCodeDetector d = new DuplicateCodeDetector(3);
        String code = "class C {"
                + " void m1() { int a=1; int b=2; int c=3; }"
                + " void m2() { int a=1; int b=2; int c=3; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "Exactly min duplicate statements should be detected");
    }

    @Test
    void belowMinStatementsNotDetected() {
        // Methods have only (minDuplicateStatements - 1) statements — window cannot form
        DuplicateCodeDetector d = new DuplicateCodeDetector(3);
        String code = "class C {"
                + " void m1() { int a=1; int b=2; }"
                + " void m2() { int a=1; int b=2; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "Window of 3 cannot form from 2 statements");
    }

    @Test
    void singleMethodProducesNoSmell() {
        // Only one method — a window can only appear once, no duplicate
        DuplicateCodeDetector d = new DuplicateCodeDetector(2);
        String code = "class C {"
                + " void m1() { int a=1; int b=2; int c=3; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "Single method has no duplicate");
    }

    @Test
    void detectedSmellHasCorrectType() {
        DuplicateCodeDetector d = new DuplicateCodeDetector(2);
        String code = "class C {"
                + " void a() { int x=1; int y=2; int z=3; }"
                + " void b() { int x=1; int y=2; int z=3; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        var smells = d.detect(cu);
        assertFalse(smells.isEmpty());
        assertEquals(SmellType.DUPLICATE_CODE, smells.get(0).getType());
    }

    @Test
    void methodWithFewerStatementsThanWindowProducesNoSmell() {
        DuplicateCodeDetector d = new DuplicateCodeDetector(5);
        String code = "class C {"
                + " void m1() { int a=1; int b=2; }"
                + " void m2() { int a=1; int b=2; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty());
    }
}
