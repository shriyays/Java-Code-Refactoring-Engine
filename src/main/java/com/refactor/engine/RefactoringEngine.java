package com.refactor.engine;

import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.report.AnalysisReport;
import com.refactor.smell.*;
import com.refactor.transform.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Central orchestrator for the Java Code Refactoring Engine.
 *
 * Usage:
 *   RefactoringEngine engine = new RefactoringEngine();
 *   AnalysisReport report = engine.analyseAndRefactor(Paths.get("src/main/java"));
 *   System.out.println(report.summary());
 */
public class RefactoringEngine {

    private final List<SmellDetector> detectors;
    private final List<Refactorer>    refactorers;

    public RefactoringEngine() {
        this.detectors = new ArrayList<>(Arrays.asList(
                new LongMethodDetector(),
                new LargeClassDetector(),
                new LongParameterListDetector(),
                new DuplicateCodeDetector(),
                new GodClassDetector(),
                new PrimitiveObsessionDetector()
        ));

        this.refactorers = new ArrayList<>(Arrays.asList(
                new LongMethodRefactorer(),
                new LongParameterListRefactorer(),
                new GodClassRefactorer()
        ));
    }

    /** Constructor for dependency injection (testing). */
    public RefactoringEngine(List<SmellDetector> detectors, List<Refactorer> refactorers) {
        this.detectors   = new ArrayList<>(detectors);
        this.refactorers = new ArrayList<>(refactorers);
    }

    /**
     * Analyse all .java files under the given root and apply refactorings.
     *
     * @param root directory to scan recursively
     * @return aggregated report
     */
    public AnalysisReport analyseAndRefactor(Path root) throws IOException {
        AnalysisReport report = new AnalysisReport();
        List<Path> javaFiles = collectJavaFiles(root);

        for (Path file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                List<CodeSmell> smells = detectSmells(cu);
                report.addSmells(smells);

                List<RefactorResult> results = applyRefactorings(cu, smells);
                report.addResults(results);
            } catch (Exception e) {
                // Log and continue — one bad file should not halt the whole run
                System.err.println("Warning: could not process " + file + ": " + e.getMessage());
            }
        }

        return report;
    }

    /**
     * Analyse a single CompilationUnit (useful for testing).
     * Runs a single pass — use analyseAndRefactorUntilClean for multi-pass.
     */
    public AnalysisReport analyseAndRefactorUnit(CompilationUnit cu) {
        AnalysisReport report = new AnalysisReport();
        List<CodeSmell> smells = detectSmells(cu);
        report.addSmells(smells);
        List<RefactorResult> results = applyRefactorings(cu, smells);
        report.addResults(results);
        return report;
    }

    /**
     * Repeatedly detect and refactor until no smells remain or maxPasses is reached.
     * Each pass fixes smells that were introduced by the previous pass
     * (e.g. a parameter-object class triggering Primitive Obsession).
     *
     * @param cu        the compilation unit to transform (mutated in place)
     * @param maxPasses maximum number of fix iterations (default: 5)
     * @return aggregated report across all passes, plus pass metadata
     */
    public AnalysisReport analyseAndRefactorUntilClean(CompilationUnit cu, int maxPasses) {
        AnalysisReport combined = new AnalysisReport();
        int pass = 0;

        while (pass < maxPasses) {
            List<CodeSmell> smells = detectSmells(cu);
            if (smells.isEmpty()) break;

            combined.addSmells(smells);
            List<RefactorResult> results = applyRefactorings(cu, smells);
            combined.addResults(results);

            // If nothing was actually refactored this pass, further passes won't help
            boolean anyFixed = results.stream().anyMatch(RefactorResult::isSuccess);
            if (!anyFixed) break;

            pass++;
        }

        return combined;
    }

    /**
     * Run only the detection phase on a single CompilationUnit.
     */
    public List<CodeSmell> detectSmells(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();
        for (SmellDetector detector : detectors) {
            smells.addAll(detector.detect(cu));
        }
        return smells;
    }

    /**
     * Apply registered refactorers for each detected smell.
     */
    public List<RefactorResult> applyRefactorings(CompilationUnit cu, List<CodeSmell> smells) {
        List<RefactorResult> results = new ArrayList<>();
        for (CodeSmell smell : smells) {
            for (Refactorer refactorer : refactorers) {
                if (refactorer.canHandle(smell)) {
                    results.add(refactorer.refactor(cu, smell));
                    break; // one refactorer per smell
                }
            }
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<Path> collectJavaFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.exists(root)) return files;

        Files.walk(root)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(files::add);

        return files;
    }

    // -------------------------------------------------------------------------
    // Entry point for standalone CLI usage
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Path target = args.length > 0 ? Paths.get(args[0]) : Paths.get("src/main/java");
        System.out.println("Scanning: " + target.toAbsolutePath());

        RefactoringEngine engine = new RefactoringEngine();
        AnalysisReport    report = engine.analyseAndRefactor(target);

        System.out.println(report.summary());
    }
}
