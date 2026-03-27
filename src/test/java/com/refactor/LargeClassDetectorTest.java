package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;
import com.refactor.smell.LargeClassDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LargeClassDetectorTest {

    // ---- Test 7 ----
    @Test
    void detectsClassWithTooManyFields() {
        LargeClassDetector detector = new LargeClassDetector(3, 20);
        String code = """
                class Big {
                    int a; int b; int c; int d;
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);
        assertTrue(smells.stream().anyMatch(s -> s.getType() == SmellType.LARGE_CLASS),
                "Should flag class with 4 fields when threshold is 3");
    }

    // ---- Test 8 ----
    @Test
    void doesNotFlagSmallClass() {
        LargeClassDetector detector = new LargeClassDetector(10, 20);
        String code = """
                class Small {
                    int a;
                    void m() {}
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Small class should not be flagged");
    }

    // ---- Test 9 ----
    @Test
    void interfacesAreIgnored() {
        LargeClassDetector detector = new LargeClassDetector(0, 0);
        String code = """
                interface MyInterface {
                    void methodA();
                    void methodB();
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Interfaces should not be flagged");
    }
}
