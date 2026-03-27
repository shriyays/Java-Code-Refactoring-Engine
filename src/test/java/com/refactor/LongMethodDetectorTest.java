package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;
import com.refactor.smell.LongMethodDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LongMethodDetectorTest {

    private static final int THRESHOLD = 5;
    private final LongMethodDetector detector = new LongMethodDetector(THRESHOLD);

    // ---- Test 1 ----
    @Test
    void detectsLongMethod() {
        String code = """
                class Foo {
                    void longMethod() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                        int f = 6;
                    }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);

        assertFalse(smells.isEmpty(), "Should detect long method");
        assertEquals(SmellType.LONG_METHOD, smells.get(0).getType());
        assertEquals("Foo", smells.get(0).getClassName());
        assertEquals("longMethod", smells.get(0).getMemberName());
    }

    // ---- Test 2 ----
    @Test
    void noSmellForShortMethod() {
        String code = """
                class Foo {
                    void shortMethod() {
                        int a = 1;
                        int b = 2;
                    }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Short method should not be flagged");
    }

    // ---- Test 3 ----
    @Test
    void detectsMultipleLongMethodsInSameClass() {
        String code = """
                class Foo {
                    void m1() { int a=1; int b=2; int c=3; int d=4; int e=5; int f=6; }
                    void m2() { int a=1; int b=2; int c=3; int d=4; int e=5; int f=6; }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);
        assertEquals(2, smells.size(), "Both methods should be flagged");
    }

    // ---- Test 4 ----
    @Test
    void methodAtExactThresholdIsNotFlagged() {
        // threshold=5, method has exactly 5 statements → not > threshold
        String code = """
                class Foo {
                    void borderMethod() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                    }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Method at exact threshold should not be flagged");
    }

    // ---- Test 5 ----
    @Test
    void abstractMethodIsIgnored() {
        String code = """
                abstract class Foo {
                    abstract void noBody();
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Abstract method without body should be ignored");
    }

    // ---- Test 6 ----
    @Test
    void customThresholdIsRespected() {
        LongMethodDetector strict = new LongMethodDetector(2);
        String code = """
                class Foo {
                    void m() { int a=1; int b=2; int c=3; }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertFalse(strict.detect(cu).isEmpty(), "Custom threshold of 2 should flag 3-statement method");
    }
}
