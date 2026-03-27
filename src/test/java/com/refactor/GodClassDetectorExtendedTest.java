package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.smell.GodClassDetector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targets surviving mutants in GodClassDetector:
 * - conditional boundary at line 52 (>= comparisons for fields/methods/loc)
 * - interface exclusion (line 46)
 * - lambda totalStatements return
 */
class GodClassDetectorExtendedTest {

    @Test
    void interfaceWithManyMembersIsNotFlagged() {
        GodClassDetector d = new GodClassDetector(0, 0, 0);
        String code = "interface Big { void a(); void b(); void c(); }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "Interface must never be flagged as god class");
    }

    @Test
    void classWithFieldsEqualToThresholdNotFlagged() {
        // fields >= threshold required — exactly at threshold should be flagged
        GodClassDetector d = new GodClassDetector(2, 2, 0);
        String code = "class C { int a; int b; void x(){} void y(){} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        // 2 fields >= 2, 2 methods >= 2, 0 loc >= 0 → should be detected
        assertFalse(d.detect(cu).isEmpty(), "All three conditions met at exact thresholds");
    }

    @Test
    void classWithOneFewerFieldThanThresholdNotFlagged() {
        // fields threshold is 3, class has 2 fields
        GodClassDetector d = new GodClassDetector(3, 2, 0);
        String code = "class C { int a; int b; void x(){} void y(){} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "Fields below threshold → not a god class");
    }

    @Test
    void classWithOneFewerMethodThanThresholdNotFlagged() {
        GodClassDetector d = new GodClassDetector(2, 3, 0);
        String code = "class C { int a; int b; void x(){} void y(){} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(d.detect(cu).isEmpty(), "Methods below threshold → not a god class");
    }

    @Test
    void totalStatementsCountedAcrossAllMethods() {
        // loc threshold = 3; 2 methods with 2 stmts each = 4 total
        GodClassDetector d = new GodClassDetector(2, 2, 3);
        String code = "class C {"
                + " int a; int b;"
                + " void x() { int i=1; int j=2; }"
                + " void y() { int i=1; int j=2; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        // 4 total statements >= 3 → flagged
        assertFalse(d.detect(cu).isEmpty(), "Total statements across methods should exceed loc threshold");
    }

    @Test
    void classWithLocExactlyAtThresholdIsFlagged() {
        // loc threshold = 4; exactly 4 total statements → should be flagged (>=)
        GodClassDetector d = new GodClassDetector(2, 2, 4);
        String code = "class C {"
                + " int a; int b;"
                + " void x() { int i=1; int j=2; }"
                + " void y() { int i=1; int j=2; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(d.detect(cu).isEmpty(), "Exactly at loc threshold → flagged");
    }
}
