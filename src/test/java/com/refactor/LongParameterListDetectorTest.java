package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;
import com.refactor.smell.LongParameterListDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LongParameterListDetectorTest {

    private final LongParameterListDetector detector = new LongParameterListDetector(3);

    // ---- Test 10 ----
    @Test
    void detectsLongParameterList() {
        String code = """
                class Foo {
                    void m(int a, int b, int c, int d) {}
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);
        assertFalse(smells.isEmpty());
        assertEquals(SmellType.LONG_PARAMETER_LIST, smells.get(0).getType());
    }

    // ---- Test 11 ----
    @Test
    void doesNotFlagAcceptableParameterCount() {
        String code = """
                class Foo {
                    void m(int a, int b) {}
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty());
    }

    // ---- Test 12 ----
    @Test
    void detectsCorrectMethodName() {
        String code = """
                class Foo {
                    void process(String x, String y, String z, String w) {}
                    void simple(int a) {}
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);
        assertEquals(1, smells.size());
        assertEquals("process", smells.get(0).getMemberName());
    }
}
