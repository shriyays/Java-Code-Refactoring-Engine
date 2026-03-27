package com.refactor.report;

import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates detection and refactoring results across all analysed files.
 */
public class AnalysisReport {

    private final List<CodeSmell>    detectedSmells   = new ArrayList<>();
    private final List<RefactorResult> refactorResults = new ArrayList<>();

    public void addSmell(CodeSmell smell)              { detectedSmells.add(smell); }
    public void addSmells(List<CodeSmell> smells)      { detectedSmells.addAll(smells); }
    public void addResult(RefactorResult result)       { refactorResults.add(result); }
    public void addResults(List<RefactorResult> results) { refactorResults.addAll(results); }

    public List<CodeSmell>     getDetectedSmells()  { return Collections.unmodifiableList(detectedSmells); }
    public List<RefactorResult> getRefactorResults() { return Collections.unmodifiableList(refactorResults); }

    public int totalSmells()         { return detectedSmells.size(); }
    public int totalRefactored()     { return (int) refactorResults.stream().filter(RefactorResult::isSuccess).count(); }
    public int totalSkipped()        { return (int) refactorResults.stream().filter(r -> !r.isSuccess()).count(); }

    /** Refactor rate as a percentage of smells that were successfully addressed. */
    public double refactorRate() {
        if (detectedSmells.isEmpty()) return 0.0;
        return (totalRefactored() * 100.0) / detectedSmells.size();
    }

    /** Count of smells per type. */
    public Map<SmellType, Long> smellsByType() {
        return detectedSmells.stream()
                .collect(Collectors.groupingBy(CodeSmell::getType, Collectors.counting()));
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Analysis Report ===\n");
        sb.append("Total smells detected : ").append(totalSmells()).append("\n");
        sb.append("Successfully refactored: ").append(totalRefactored()).append("\n");
        sb.append("Skipped               : ").append(totalSkipped()).append("\n");
        sb.append(String.format("Refactor rate         : %.1f%%\n", refactorRate()));
        sb.append("\n--- Smells by Type ---\n");
        smellsByType().forEach((type, count) ->
                sb.append("  ").append(type.getDisplayName()).append(": ").append(count).append("\n"));
        sb.append("\n--- Detected Smells ---\n");
        detectedSmells.forEach(s -> sb.append("  ").append(s).append("\n"));
        sb.append("\n--- Refactoring Results ---\n");
        refactorResults.forEach(r -> sb.append("  ").append(r).append("\n"));
        return sb.toString();
    }
}
