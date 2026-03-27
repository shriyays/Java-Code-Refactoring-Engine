package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.transform.LongParameterListRefactorer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LongParameterListRefactorerTest {

    private final LongParameterListRefactorer refactorer = new LongParameterListRefactorer();

    private CodeSmell smell(String cls, String method) {
        return new CodeSmell(SmellType.LONG_PARAMETER_LIST, cls, method, 1, "test");
    }

    @Test
    void canHandleOnlyLongParameterList() {
        assertTrue(refactorer.canHandle(smell("A", "m")));
        assertFalse(refactorer.canHandle(
                new CodeSmell(SmellType.LONG_METHOD, "A", "m", 1, "x")));
        assertFalse(refactorer.canHandle(
                new CodeSmell(SmellType.LARGE_CLASS, "A", null, 1, "x")));
    }

    @Test
    void introducesParameterObjectSuccessfully() {
        String code = """
                class Foo {
                    void process(String name, int age, String email, boolean active) {}
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "process"));

        assertTrue(result.isSuccess(), "Should succeed: " + result.getMessage());
        assertEquals("Foo", result.getClassName());
        assertEquals(SmellType.LONG_PARAMETER_LIST, result.getSmellAddressed());
        assertTrue(cu.toString().contains("ProcessParams"),
                "Parameter object class should be created");
    }

    @Test
    void returnsFailureForMissingClass() {
        String code = "class Foo { void m(int a, int b, int c) {} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Missing", "m"));
        assertFalse(result.isSuccess());
        assertNull(result.getRefactoredCode());
    }

    @Test
    void returnsFailureForMissingMethod() {
        String code = "class Foo { void m(int a, int b, int c) {} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "noSuchMethod"));
        assertFalse(result.isSuccess());
    }

    @Test
    void returnsFailureForSingleParameter() {
        String code = "class Foo { void m(int a) {} }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "m"));
        assertFalse(result.isSuccess());
    }

    @Test
    void parameterObjectClassNameIsCapitalized() {
        String code = """
                class Foo {
                    void doSomething(int x, int y, int z) {}
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "doSomething"));
        assertTrue(result.isSuccess());
        assertTrue(cu.toString().contains("DoSomethingParams"));
    }

    @Test
    void refactoredCodeIsNotNull() {
        String code = """
                class Foo {
                    void go(int a, int b, String c) {}
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "go"));
        assertTrue(result.isSuccess());
        assertNotNull(result.getRefactoredCode());
        assertFalse(result.getRefactoredCode().isBlank());
    }
}
