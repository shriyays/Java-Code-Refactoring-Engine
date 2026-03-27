package com.refactor;

import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.report.AnalysisReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisReportTest {

    @Test
    void refactorRateIsZeroWhenNoSmells() {
        AnalysisReport report = new AnalysisReport();
        assertEquals(0.0, report.refactorRate(), 0.001);
    }

    @Test
    void refactorRateCalculatedCorrectly() {
        AnalysisReport report = new AnalysisReport();
        report.addSmell(new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, ""));
        report.addSmell(new CodeSmell(SmellType.LARGE_CLASS, "B", null, 1, ""));
        report.addResult(new RefactorResult("A", SmellType.LONG_METHOD, true, "", "ok"));
        report.addResult(new RefactorResult("B", SmellType.LARGE_CLASS, false, null, "skipped"));

        assertEquals(50.0, report.refactorRate(), 0.001,
                "1 of 2 smells refactored = 50%");
    }

    @Test
    void summaryContainsKeyLabels() {
        AnalysisReport report = new AnalysisReport();
        String summary = report.summary();
        assertTrue(summary.contains("Total smells detected"));
        assertTrue(summary.contains("Refactor rate"));
    }
}
