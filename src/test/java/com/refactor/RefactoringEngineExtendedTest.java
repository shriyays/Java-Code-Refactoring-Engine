package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.engine.RefactoringEngine;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.report.AnalysisReport;
import com.refactor.smell.*;
import com.refactor.transform.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extra coverage for RefactoringEngine: dependency-injected construction,
 * applyRefactorings branching, and multi-smell detection.
 */
class RefactoringEngineExtendedTest {

    @Test
    void customConstructorUsesProvidedDetectorsAndRefactorers() {
        LongMethodDetector detector = new LongMethodDetector(2);
        LongMethodRefactorer refactorer = new LongMethodRefactorer();
        RefactoringEngine engine = new RefactoringEngine(List.of(detector), List.of(refactorer));

        // 5 statements > threshold 2 → smell detected; refactorer needs ≥4 to extract
        String code = "class X { void m() { int a=1; int b=2; int c=3; int d=4; int e=5; } }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        AnalysisReport report = engine.analyseAndRefactorUnit(cu);

        assertEquals(1, report.totalSmells());
        assertEquals(1, report.totalRefactored());
    }

    @Test
    void noSmellsProducesNoRefactorResults() {
        RefactoringEngine engine = new RefactoringEngine();
        String code = "class Clean { void small() { int x = 1; } }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        AnalysisReport report = engine.analyseAndRefactorUnit(cu);

        assertEquals(0, report.totalSmells());
        assertEquals(0, report.getRefactorResults().size());
        assertEquals(0.0, report.refactorRate(), 0.001);
    }

    @Test
    void smellWithNoMatchingRefactorerProducesNoResult() {
        // Use only LargeClassDetector but no refactorer that handles LARGE_CLASS
        LargeClassDetector detector = new LargeClassDetector(0, 0);
        RefactoringEngine engine = new RefactoringEngine(
                List.of(detector),
                List.of(new LongMethodRefactorer())  // handles LONG_METHOD only
        );
        String code = "class C { void m() {} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        AnalysisReport report = engine.analyseAndRefactorUnit(cu);

        // Smell detected but no matching refactorer — result list should be empty
        assertTrue(report.totalSmells() > 0);
        assertEquals(0, report.getRefactorResults().size());
    }

    @Test
    void detectSmellsReturnsAllSmellTypes() {
        RefactoringEngine engine = new RefactoringEngine();
        // Code triggers LONG_METHOD + LONG_PARAMETER_LIST
        String code = "class M {"
                + " void big(int a, int b, int c, int d, int e) {"
                + "  int x1=1;int x2=2;int x3=3;int x4=4;int x5=5;"
                + "  int x6=6;int x7=7;int x8=8;int x9=9;int x10=10;"
                + "  int x11=11;int x12=12;int x13=13;int x14=14;int x15=15;"
                + "  int x16=16;int x17=17;int x18=18;int x19=19;int x20=20;"
                + "  int x21=21;"
                + " }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = engine.detectSmells(cu);

        assertTrue(smells.stream().anyMatch(s -> s.getType() == SmellType.LONG_METHOD));
        assertTrue(smells.stream().anyMatch(s -> s.getType() == SmellType.LONG_PARAMETER_LIST));
    }

    @Test
    void applyRefactoringsReturnsResultPerHandledSmell() {
        RefactoringEngine engine = new RefactoringEngine();
        String code = "class R {"
                + " void longOne() {"
                + "  int a=1;int b=2;int c=3;int d=4;int e=5;"
                + "  int f=6;int g=7;int h=8;int i=9;int j=10;"
                + "  int k=11;int l=12;int m=13;int n=14;int o=15;"
                + "  int p=16;int q=17;int r=18;int s=19;int t=20;int u=21;"
                + " }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = engine.detectSmells(cu);
        List<RefactorResult> results = engine.applyRefactorings(cu, smells);

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(RefactorResult::isSuccess));
    }
}
