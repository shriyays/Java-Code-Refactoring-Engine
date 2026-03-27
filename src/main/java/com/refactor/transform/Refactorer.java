package com.refactor.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;

/**
 * Contract for all refactoring transformations.
 */
public interface Refactorer {

    /**
     * Apply a transformation to the compilation unit to address the given smell.
     *
     * @param cu    the parsed source (mutated in place)
     * @param smell the smell to fix
     * @return result describing success/failure and any generated code
     */
    RefactorResult refactor(CompilationUnit cu, CodeSmell smell);

    /**
     * @return true if this refactorer can handle the given smell type
     */
    boolean canHandle(CodeSmell smell);
}
