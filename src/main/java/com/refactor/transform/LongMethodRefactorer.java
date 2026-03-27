package com.refactor.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.VoidType;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Refactors Long Methods by extracting the second half of statements
 * into a new private helper method (Extract Method pattern).
 *
 * Strategy:
 *   1. Find the target method in the class.
 *   2. Split statements at the midpoint.
 *   3. Create a new private void helper containing the latter half.
 *   4. Replace the latter half in the original method with a call to the helper.
 */
public class LongMethodRefactorer implements Refactorer {

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == SmellType.LONG_METHOD;
    }

    @Override
    public RefactorResult refactor(CompilationUnit cu, CodeSmell smell) {
        String targetClass  = smell.getClassName();
        String targetMethod = smell.getMemberName();

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(c -> c.getNameAsString().equals(targetClass))
                .findFirst();

        if (classOpt.isEmpty()) {
            return new RefactorResult(targetClass, SmellType.LONG_METHOD, false, null,
                    "Class not found: " + targetClass);
        }

        ClassOrInterfaceDeclaration cls = classOpt.get();

        Optional<MethodDeclaration> methodOpt = cls.getMethods().stream()
                .filter(m -> m.getNameAsString().equals(targetMethod) && m.getBody().isPresent())
                .findFirst();

        if (methodOpt.isEmpty()) {
            return new RefactorResult(targetClass, SmellType.LONG_METHOD, false, null,
                    "Method not found: " + targetMethod);
        }

        MethodDeclaration method = methodOpt.get();
        BlockStmt body = method.getBody().get();
        List<Statement> statements = new ArrayList<>(body.getStatements());

        if (statements.size() < 4) {
            return new RefactorResult(targetClass, SmellType.LONG_METHOD, false, null,
                    "Method too short to extract meaningfully");
        }

        int splitPoint = statements.size() / 2;
        List<Statement> firstHalf  = statements.subList(0, splitPoint);
        List<Statement> secondHalf = statements.subList(splitPoint, statements.size());

        // Build helper method name (effectively final for lambda use)
        final String baseHelperName = targetMethod + "Helper";
        long existing = cls.getMethods().stream()
                .filter(m -> m.getNameAsString().startsWith(baseHelperName))
                .count();
        final String helperName = existing > 0 ? baseHelperName + existing : baseHelperName;

        // Create helper method
        MethodDeclaration helper = new MethodDeclaration();
        helper.setName(helperName);
        helper.setType(new VoidType());
        helper.addModifier(com.github.javaparser.ast.Modifier.Keyword.PRIVATE);
        BlockStmt helperBody = new BlockStmt();
        secondHalf.forEach(s -> helperBody.addStatement(s.clone()));
        helper.setBody(helperBody);

        // Rebuild original method body with first half + call to helper
        BlockStmt newBody = new BlockStmt();
        firstHalf.forEach(s -> newBody.addStatement(s.clone()));
        newBody.addStatement(com.github.javaparser.StaticJavaParser.parseStatement(helperName + "();"));
        method.setBody(newBody);

        // Add helper to class
        cls.addMember(helper);

        return new RefactorResult(targetClass, SmellType.LONG_METHOD, true, cu.toString(),
                "Extracted '" + helperName + "' from '" + targetMethod + "'");
    }
}
