package com.refactor;

import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CodeSmell, RefactorResult, and SmellType model classes.
 * These drive mutation coverage on getters, constructors, and toString.
 */
class SmellModelTest {

    @Test
    void codeSmellGettersReturnConstructedValues() {
        CodeSmell smell = new CodeSmell(SmellType.LONG_METHOD, "MyClass", "myMethod", 42, "too long");
        assertEquals(SmellType.LONG_METHOD, smell.getType());
        assertEquals("MyClass",  smell.getClassName());
        assertEquals("myMethod", smell.getMemberName());
        assertEquals(42,         smell.getLine());
        assertEquals("too long", smell.getDetail());
    }

    @Test
    void codeSmellToStringContainsKeyInfo() {
        CodeSmell smell = new CodeSmell(SmellType.LARGE_CLASS, "BigClass", null, 5, "many fields");
        String s = smell.toString();
        assertTrue(s.contains("Large Class"));
        assertTrue(s.contains("BigClass"));
        assertTrue(s.contains("5"));
    }

    @Test
    void codeSmellToStringWithMemberName() {
        CodeSmell smell = new CodeSmell(SmellType.LONG_METHOD, "Foo", "bar", 10, "detail");
        assertTrue(smell.toString().contains("Foo#bar"));
    }

    @Test
    void refactorResultGettersReturnConstructedValues() {
        RefactorResult result = new RefactorResult("Foo", SmellType.LONG_METHOD, true, "code", "ok");
        assertEquals("Foo",               result.getClassName());
        assertEquals(SmellType.LONG_METHOD, result.getSmellAddressed());
        assertTrue(result.isSuccess());
        assertEquals("code", result.getRefactoredCode());
        assertEquals("ok",   result.getMessage());
    }

    @Test
    void refactorResultFailureIsSuccess() {
        RefactorResult result = new RefactorResult("Foo", SmellType.LARGE_CLASS, false, null, "skipped");
        assertFalse(result.isSuccess());
        assertNull(result.getRefactoredCode());
    }

    @Test
    void refactorResultToStringContainsStatus() {
        RefactorResult ok   = new RefactorResult("A", SmellType.LONG_METHOD, true,  "c", "msg");
        RefactorResult skip = new RefactorResult("B", SmellType.LARGE_CLASS, false, null, "msg");
        assertTrue(ok.toString().contains("REFACTORED"));
        assertTrue(skip.toString().contains("SKIPPED"));
    }

    @Test
    void smellTypeDisplayNameAndDescriptionAreSet() {
        for (SmellType t : SmellType.values()) {
            assertNotNull(t.getDisplayName(),  t.name() + " displayName");
            assertFalse(t.getDisplayName().isBlank(), t.name() + " displayName blank");
            assertNotNull(t.getDescription(),  t.name() + " description");
            assertFalse(t.getDescription().isBlank(), t.name() + " description blank");
        }
    }
}
