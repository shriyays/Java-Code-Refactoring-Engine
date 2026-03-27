package com.refactor.model;

/**
 * Holds the outcome of a single refactoring transformation.
 */
public class RefactorResult {

    private final String className;
    private final SmellType smellAddressed;
    private final boolean success;
    private final String refactoredCode;
    private final String message;

    public RefactorResult(String className, SmellType smellAddressed,
                          boolean success, String refactoredCode, String message) {
        this.className = className;
        this.smellAddressed = smellAddressed;
        this.success = success;
        this.refactoredCode = refactoredCode;
        this.message = message;
    }

    public String getClassName()          { return className; }
    public SmellType getSmellAddressed()  { return smellAddressed; }
    public boolean isSuccess()            { return success; }
    public String getRefactoredCode()     { return refactoredCode; }
    public String getMessage()            { return message; }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s",
                smellAddressed.getDisplayName(), className,
                success ? "REFACTORED" : "SKIPPED", message);
    }
}
