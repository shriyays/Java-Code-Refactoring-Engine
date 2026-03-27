package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.transform.GodClassRefactorer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GodClassRefactorerTest {

    private final GodClassRefactorer refactorer = new GodClassRefactorer();

    private CodeSmell smell(String cls) {
        return new CodeSmell(SmellType.GOD_CLASS, cls, null, 1, "god class");
    }

    @Test
    void canHandleOnlyGodClassSmell() {
        assertTrue(refactorer.canHandle(smell("A")));
        assertFalse(refactorer.canHandle(
                new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, "x")));
        assertFalse(refactorer.canHandle(
                new CodeSmell(SmellType.LARGE_CLASS, "A", null, 1, "x")));
    }

    @Test
    void extractsDelegateClassSuccessfully() {
        String code = "class Monster {"
                + " int a; int b; int c; int d;"
                + " void m1() { int x=1; }"
                + " void m2() { int x=2; }"
                + " void m3() { int x=3; }"
                + " void m4() { int x=4; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Monster"));

        assertTrue(result.isSuccess(), "Should succeed: " + result.getMessage());
        assertEquals("Monster", result.getClassName());
        assertEquals(SmellType.GOD_CLASS, result.getSmellAddressed());
        assertTrue(cu.toString().contains("MonsterDelegate"),
                "Delegate class should be created");
        assertNotNull(result.getRefactoredCode());
    }

    @Test
    void returnsFailureForMissingClass() {
        String code = "class Foo { int a; void m() {} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("NonExistent"));
        assertFalse(result.isSuccess());
        assertNull(result.getRefactoredCode());
    }

    @Test
    void returnsFailureWhenNotEnoughMembers() {
        // Only 1 field, 1 method — can't split into a meaningful delegate
        String code = "class Tiny { int a; void m() {} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Tiny"));
        assertFalse(result.isSuccess());
    }

    @Test
    void delegateFieldAddedToOriginalClass() {
        String code = "class Big {"
                + " int a; int b; int c;"
                + " void x() { int i=1; }"
                + " void y() { int i=2; }"
                + " void z() { int i=3; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Big"));
        assertTrue(result.isSuccess());
        assertTrue(cu.toString().contains("delegate"), "Delegation field should be present");
    }

    @Test
    void messageDescribesExtraction() {
        String code = "class G {"
                + " int a; int b;"
                + " void p() { int x=1; }"
                + " void q() { int x=2; }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("G"));
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("GDelegate"));
    }
}
