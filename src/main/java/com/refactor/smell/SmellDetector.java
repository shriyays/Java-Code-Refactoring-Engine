package com.refactor.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;

import java.util.List;

/**
 * Contract for all code-smell detectors.
 */
public interface SmellDetector {

    /**
     * Analyse the given compilation unit and return every smell instance found.
     *
     * @param cu the parsed Java source file
     * @return list of detected smells (empty if none)
     */
    List<CodeSmell> detect(CompilationUnit cu);
}
