package com.refactor;

import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.report.AnalysisReport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for AnalysisReport to kill surviving mutants on
 * totalRefactored, totalSkipped, smellsByType, and addResults.
 */
class AnalysisReportExtendedTest {

    @Test
    void totalSmellsCountsAllAdded() {
        AnalysisReport report = new AnalysisReport();
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, ""));
        report.addSmell(new CodeSmell(SmellType.LARGE_CLASS, "B", null, 1, ""));
        report.addSmell(new CodeSmell(SmellType.GOD_CLASS, "C", null, 1, ""));
        assertEquals(3, report.totalSmells());
    }

    @Test
    void totalRefactoredCountsOnlySuccesses() {
        AnalysisReport report = new AnalysisReport();
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, ""));
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "B", "n", 1, ""));
        report.addResult(new RefactorResult("A", SmellType.LONG_METHOD, true,  "c", "ok"));
        report.addResult(new RefactorResult("B", SmellType.LONG_METHOD, false, null, "skip"));
        assertEquals(1, report.totalRefactored());
        assertEquals(1, report.totalSkipped());
    }

    @Test
    void totalSkippedCountsOnlyFailures() {
        AnalysisReport report = new AnalysisReport();
        report.addResult(new RefactorResult("X", SmellType.LARGE_CLASS, false, null, "s"));
        report.addResult(new RefactorResult("Y", SmellType.LARGE_CLASS, false, null, "s"));
        assertEquals(2, report.totalSkipped());
        assertEquals(0, report.totalRefactored());
    }

    @Test
    void refactorRateIsHundredPercentWhenAllRefactored() {
        AnalysisReport report = new AnalysisReport();
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, ""));
        report.addResult(new RefactorResult("A", SmellType.LONG_METHOD, true, "c", "ok"));
        assertEquals(100.0, report.refactorRate(), 0.001);
    }

    @Test
    void refactorRateIsZeroWhenAllSkipped() {
        AnalysisReport report = new AnalysisReport();
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, ""));
        report.addResult(new RefactorResult("A", SmellType.LONG_METHOD, false, null, "skip"));
        assertEquals(0.0, report.refactorRate(), 0.001);
    }

    @Test
    void smellsByTypeGroupsCorrectly() {
        AnalysisReport report = new AnalysisReport();
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "A", "m1", 1, ""));
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "A", "m2", 2, ""));
        report.addSmell(new CodeSmell(SmellType.LARGE_CLASS, "B", null, 3, ""));

        Map<SmellType, Long> byType = report.smellsByType();
        assertEquals(2L, byType.get(SmellType.LONG_METHOD));
        assertEquals(1L, byType.get(SmellType.LARGE_CLASS));
        assertNull(byType.get(SmellType.GOD_CLASS));
    }

    @Test
    void addSmellsListAddsAll() {
        AnalysisReport report = new AnalysisReport();
        report.addSmells(java.util.List.of(
                new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, ""),
                new CodeSmell(SmellType.DUPLICATE_CODE, "B", "n", 2, "")
        ));
        assertEquals(2, report.totalSmells());
        assertEquals(2, report.getDetectedSmells().size());
    }

    @Test
    void addResultsListAddsAll() {
        AnalysisReport report = new AnalysisReport();
        report.addResults(java.util.List.of(
                new RefactorResult("A", SmellType.LONG_METHOD, true,  "c", "ok"),
                new RefactorResult("B", SmellType.LARGE_CLASS, false, null, "skip")
        ));
        assertEquals(2, report.getRefactorResults().size());
        assertEquals(1, report.totalRefactored());
        assertEquals(1, report.totalSkipped());
    }

    @Test
    void detectedSmellsListIsUnmodifiable() {
        AnalysisReport report = new AnalysisReport();
        assertThrows(UnsupportedOperationException.class,
                () -> report.getDetectedSmells().add(
                        new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, "")));
    }

    @Test
    void refactorResultsListIsUnmodifiable() {
        AnalysisReport report = new AnalysisReport();
        assertThrows(UnsupportedOperationException.class,
                () -> report.getRefactorResults().add(
                        new RefactorResult("A", SmellType.LONG_METHOD, true, "c", "ok")));
    }
}
