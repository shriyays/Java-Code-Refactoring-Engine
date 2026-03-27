package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;
import com.refactor.smell.GodClassDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GodClassDetectorTest {

    // ---- Test 13 ----
    @Test
    void detectsGodClass() {
        GodClassDetector detector = new GodClassDetector(2, 2, 4);
        String code = """
                class Monster {
                    int a; int b; int c;
                    void m1() { int x=1; int y=2; int z=3; }
                    void m2() { int x=1; int y=2; }
                    void m3() { int x=1; }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);
        assertFalse(smells.isEmpty(), "Should detect god class");
        assertEquals(SmellType.GOD_CLASS, smells.get(0).getType());
    }

    // ---- Test 14 ----
    @Test
    void doesNotFlagNormalClass() {
        GodClassDetector detector = new GodClassDetector(10, 10, 100);
        String code = """
                class Normal {
                    int a;
                    void m() { int x = 1; }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Normal class should not be flagged");
    }

    // ---- Test 15 ----
    @Test
    void requiresAllThreeConditions() {
        // Many fields, few methods — NOT a god class
        GodClassDetector detector = new GodClassDetector(2, 5, 4);
        String code = """
                class PartialBig {
                    int a; int b; int c;
                    void m() { int x=1; }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Should not flag if method count condition not met");
    }
}
