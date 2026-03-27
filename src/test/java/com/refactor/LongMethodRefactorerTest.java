package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.transform.LongMethodRefactorer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LongMethodRefactorerTest {

    private final LongMethodRefactorer refactorer = new LongMethodRefactorer();

    private CodeSmell smell(String cls, String method) {
        return new CodeSmell(SmellType.LONG_METHOD, cls, method, 1, "test");
    }

    // ---- Test (part of 20) ----
    @Test
    void successfullyExtractsHelperMethod() {
        String code = """
                class Foo {
                    void longOne() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                        int d = 4;
                        int e = 5;
                        int f = 6;
                    }
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "longOne"));

        assertTrue(result.isSuccess(), "Refactoring should succeed");
        assertTrue(cu.toString().contains("longOneHelper"),
                "Helper method should be added to the class");
    }

    @Test
    void returnsFailureForMissingClass() {
        String code = "class Foo { void m() { int x=1; } }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("NonExistent", "m"));
        assertFalse(result.isSuccess());
    }

    @Test
    void canHandleOnlyLongMethodSmell() {
        assertTrue(refactorer.canHandle(smell("A", "b")));
        assertFalse(refactorer.canHandle(
                new CodeSmell(SmellType.LARGE_CLASS, "A", null, 1, "test")));
    }
}
