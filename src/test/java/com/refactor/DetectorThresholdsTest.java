package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.smell.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boundary / threshold tests — each test targets a specific conditional branch
 * to kill surviving mutants (off-by-one operators, comparisons, etc.).
 */
class DetectorThresholdsTest {

    // --- LongMethodDetector boundary ---

    @Test
    void longMethodJustOverThresholdIsDetected() {
        LongMethodDetector d = new LongMethodDetector(3);
        String code = "class C { void m() { int a=1; int b=2; int c=3; int x=4; } }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertEquals(1, d.detect(cu).size(), "4 statements > threshold 3 must be flagged");
    }

    @Test
    void longMethodExactlyAtThresholdIsNotDetected() {
        LongMethodDetector d = new LongMethodDetector(3);
        String code = "class C { void m() { int a=1; int b=2; int c=3; } }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "3 statements == threshold 3, must NOT be flagged");
    }

    @Test
    void longMethodDetectorThresholdGetter() {
        LongMethodDetector d = new LongMethodDetector(7);
        assertEquals(7, d.getThreshold());
    }

    @Test
    void longMethodDefaultThresholdIsTwenty() {
        LongMethodDetector d = new LongMethodDetector();
        assertEquals(20, d.getThreshold());
    }

    // --- LargeClassDetector boundary ---

    @Test
    void largeClassJustOverFieldThresholdDetected() {
        LargeClassDetector d = new LargeClassDetector(2, 100);
        String code = "class C { int a; int b; int c; }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "3 fields > threshold 2 must be flagged");
    }

    @Test
    void largeClassAtExactFieldThresholdNotDetected() {
        LargeClassDetector d = new LargeClassDetector(3, 100);
        String code = "class C { int a; int b; int c; }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "3 fields == threshold 3 must NOT be flagged");
    }

    @Test
    void largeClassJustOverMethodThresholdDetected() {
        LargeClassDetector d = new LargeClassDetector(100, 2);
        String code = "class C { void a(){} void b(){} void c(){} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "3 methods > threshold 2 must be flagged");
    }

    @Test
    void largeClassGetters() {
        LargeClassDetector d = new LargeClassDetector(5, 15);
        assertEquals(5,  d.getFieldThreshold());
        assertEquals(15, d.getMethodThreshold());
    }

    @Test
    void largeClassDefaultThresholds() {
        LargeClassDetector d = new LargeClassDetector();
        assertEquals(10, d.getFieldThreshold());
        assertEquals(20, d.getMethodThreshold());
    }

    // --- LongParameterListDetector boundary ---

    @Test
    void longParamJustOverThresholdDetected() {
        LongParameterListDetector d = new LongParameterListDetector(2);
        String code = "class C { void m(int a, int b, int c){} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "3 params > threshold 2 must be flagged");
    }

    @Test
    void longParamAtThresholdNotDetected() {
        LongParameterListDetector d = new LongParameterListDetector(3);
        String code = "class C { void m(int a, int b, int c){} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "3 params == threshold 3 must NOT be flagged");
    }

    @Test
    void longParamDefaultThresholdIsFour() {
        LongParameterListDetector d = new LongParameterListDetector();
        assertEquals(4, d.getThreshold());
    }

    // --- GodClassDetector boundary ---

    @Test
    void godClassGetters() {
        GodClassDetector d = new GodClassDetector(3, 5, 10);
        assertEquals(3,  d.getFieldThreshold());
        assertEquals(5,  d.getMethodThreshold());
        assertEquals(10, d.getLocThreshold());
    }

    @Test
    void godClassDefaultThresholds() {
        GodClassDetector d = new GodClassDetector();
        assertEquals(7,  d.getFieldThreshold());
        assertEquals(15, d.getMethodThreshold());
        assertEquals(80, d.getLocThreshold());
    }

    // --- PrimitiveObsessionDetector boundary ---

    @Test
    void primitiveObsessionJustOverThresholdDetected() {
        PrimitiveObsessionDetector d = new PrimitiveObsessionDetector(2);
        // 3 fields > threshold 2 → flagged
        String code = "class C { int a; String b; int c; }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "3 primitive fields > threshold 2 must be flagged");
    }

    @Test
    void primitiveObsessionAtThresholdNotDetected() {
        PrimitiveObsessionDetector d = new PrimitiveObsessionDetector(3);
        // 3 fields == threshold 3 → NOT flagged (detector uses >)
        String code = "class C { int a; String b; int c; }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "3 primitive fields == threshold 3 must NOT be flagged");
    }

    @Test
    void primitiveObsessionDefaultThresholdIsFive() {
        PrimitiveObsessionDetector d = new PrimitiveObsessionDetector();
        assertEquals(5, d.getThreshold());
    }

    // --- DuplicateCodeDetector boundary ---

    @Test
    void duplicateCodeGetterReturnsThreshold() {
        DuplicateCodeDetector d = new DuplicateCodeDetector(4);
        assertEquals(4, d.getMinDuplicateStatements());
    }
}
