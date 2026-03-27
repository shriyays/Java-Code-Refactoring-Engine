package com.refactor.model;

/**
 * Represents a detected code smell at a specific location.
 */
public class CodeSmell {

    private final SmellType type;
    private final String className;
    private final String memberName;   // method or field name, may be null
    private final int line;
    private final String detail;

    public CodeSmell(SmellType type, String className, String memberName, int line, String detail) {
        this.type = type;
        this.className = className;
        this.memberName = memberName;
        this.line = line;
        this.detail = detail;
    }

    public SmellType getType()       { return type; }
    public String getClassName()     { return className; }
    public String getMemberName()    { return memberName; }
    public int getLine()             { return line; }
    public String getDetail()        { return detail; }

    @Override
    public String toString() {
        String loc = memberName != null ? className + "#" + memberName : className;
        return String.format("[%s] %s (line %d): %s", type.getDisplayName(), loc, line, detail);
    }
}
