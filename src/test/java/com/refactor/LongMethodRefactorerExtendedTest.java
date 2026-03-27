package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;
import com.refactor.transform.LongMethodRefactorer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage for LongMethodRefactorer edge cases
 * to kill surviving mutants on helper-name uniqueness and split logic.
 */
class LongMethodRefactorerExtendedTest {

    private final LongMethodRefactorer refactorer = new LongMethodRefactorer();

    private CodeSmell smell(String cls, String method) {
        return new CodeSmell(SmellType.LONG_METHOD, cls, method, 1, "test");
    }

    @Test
    void helperAddedToClassAfterRefactoring() {
        String code = "class Foo {"
                + " void longOne() {"
                + "  int a=1; int b=2; int c=3; int d=4; int e=5; int f=6;"
                + " }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        int methodsBefore = cu.findAll(
                com.github.javaparser.ast.body.MethodDeclaration.class).size();

        refactorer.refactor(cu, smell("Foo", "longOne"));

        int methodsAfter = cu.findAll(
                com.github.javaparser.ast.body.MethodDeclaration.class).size();
        assertEquals(methodsBefore + 1, methodsAfter, "One helper method should be added");
    }

    @Test
    void originalMethodBodyIsShortenedAfterExtraction() {
        String code = "class Foo {"
                + " void big() {"
                + "  int a=1; int b=2; int c=3; int d=4; int e=5; int f=6;"
                + " }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        refactorer.refactor(cu, smell("Foo", "big"));

        // Original method should now call helper — statement count should be reduced
        var method = cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class)
                .stream().filter(m -> m.getNameAsString().equals("big")).findFirst();
        assertTrue(method.isPresent());
        // body = first half + 1 call to helper — less than original 6
        int stmts = method.get().getBody().map(b -> b.getStatements().size()).orElse(0);
        assertTrue(stmts < 6, "Original method body should be shorter after extraction");
    }

    @Test
    void returnsFailureForTooShortMethod() {
        String code = "class Foo { void m() { int a=1; int b=2; } }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "m"));
        assertFalse(result.isSuccess(), "Method with 2 statements is too short to extract");
    }

    @Test
    void refactoredCodeStringIsReturnedOnSuccess() {
        String code = "class Foo {"
                + " void big() {"
                + "  int a=1; int b=2; int c=3; int d=4; int e=5; int f=6;"
                + " }"
                + "}";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "big"));

        assertTrue(result.isSuccess());
        assertNotNull(result.getRefactoredCode());
        assertFalse(result.getRefactoredCode().isBlank());
        assertEquals("Foo", result.getClassName());
    }

    @Test
    void returnsFailureForAbstractMethod() {
        String code = "abstract class Foo { abstract void noBody(); }";
        CompilationUnit cu = StaticJavaParser.parse(code);
        RefactorResult result = refactorer.refactor(cu, smell("Foo", "noBody"));
        assertFalse(result.isSuccess());
    }
}
