package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;
import com.refactor.smell.DuplicateCodeDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateCodeDetectorTest {

    @Test
    void detectsDuplicateBlocksAcrossMethods() {
        DuplicateCodeDetector detector = new DuplicateCodeDetector(2);
        String code = """
                class Foo {
                    void m1() { int a = 1; int b = 2; int c = 3; }
                    void m2() { int a = 1; int b = 2; int c = 3; }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);
        assertFalse(smells.isEmpty(), "Duplicate block should be detected");
        assertEquals(SmellType.DUPLICATE_CODE, smells.get(0).getType());
    }

    @Test
    void noDuplicateInUniqueCode() {
        DuplicateCodeDetector detector = new DuplicateCodeDetector(2);
        String code = """
                class Foo {
                    void m1() { int a = 1; }
                    void m2() { String s = "hello"; }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "No duplicate code should be detected");
    }

    @Test
    void defaultThresholdIsThree() {
        DuplicateCodeDetector detector = new DuplicateCodeDetector();
        assertEquals(3, detector.getMinDuplicateStatements());
    }

    @Test
    void emptyClassProducesNoSmells() {
        DuplicateCodeDetector detector = new DuplicateCodeDetector(2);
        String code = "class Empty {}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty());
    }
}
