package com.refactor.model;

/**
 * Enumeration of detectable code smell categories.
 */
public enum SmellType {
    LONG_METHOD("Long Method", "Method exceeds threshold line count"),
    LARGE_CLASS("Large Class", "Class has too many fields or methods"),
    DUPLICATE_CODE("Duplicate Code", "Identical or near-identical code blocks detected"),
    LONG_PARAMETER_LIST("Long Parameter List", "Method has too many parameters"),
    GOD_CLASS("God Class", "Class handles too many responsibilities"),
    FEATURE_ENVY("Feature Envy", "Method uses more features of another class than its own"),
    DATA_CLUMPS("Data Clumps", "Same group of fields appear together repeatedly"),
    PRIMITIVE_OBSESSION("Primitive Obsession", "Excessive use of primitives instead of domain objects");

    private final String displayName;
    private final String description;

    SmellType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
