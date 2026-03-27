package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.engine.RefactoringEngine;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;
import com.refactor.report.AnalysisReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RefactoringEngineTest {

    private RefactoringEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RefactoringEngine();
    }

    // ---- Test 18 ----
    @Test
    void detectsSmellsFromCompilationUnit() {
        String code = """
                class Smelly {
                    void longOne() {
                        int a=1; int b=2; int c=3;
                        int d=4; int e=5; int f=6;
                        int g=7; int h=8; int i=9;
                        int j=10; int k=11; int l=12;
                        int m=13; int n=14; int o=15;
                        int p=16; int q=17; int r=18;
                        int s=19; int t=20; int u=21;
                    }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = engine.detectSmells(cu);
        assertTrue(smells.stream().anyMatch(s -> s.getType() == SmellType.LONG_METHOD),
                "Engine should detect long method smell");
    }

    // ---- Test 19 ----
    @Test
    void reportRefactorRateIsNonNegative() {
        String code = """
                class Clean {
                    void simple() {
                        int x = 1;
                    }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        AnalysisReport report = engine.analyseAndRefactorUnit(cu);
        assertTrue(report.refactorRate() >= 0.0, "Refactor rate must be non-negative");
    }

    // ---- Test 20 ----
    @Test
    void reportSmellsByTypeContainsDetectedTypes() {
        String code = """
                class Multi {
                    void longMethod(int a, int b, int c, int d, int e) {
                        int x=1; int y=2; int z=3;
                        int aa=4; int bb=5; int cc=6;
                        int dd=7; int ee=8; int ff=9;
                        int gg=10; int hh=11; int ii=12;
                        int jj=13; int kk=14; int ll=15;
                        int mm=16; int nn=17; int oo=18;
                        int pp=19; int qq=20; int rr=21;
                    }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        AnalysisReport report = engine.analyseAndRefactorUnit(cu);

        assertTrue(report.getDetectedSmells().size() > 0, "Should have detected at least one smell");
        assertFalse(report.smellsByType().isEmpty(), "smellsByType should not be empty");
    }
}
