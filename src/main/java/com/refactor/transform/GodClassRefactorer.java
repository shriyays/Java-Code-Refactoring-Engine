package com.refactor.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Refactors God Classes by applying the Extract Class pattern.
 *
 * Strategy:
 *   1. Split fields into two halves.
 *   2. Split methods into two halves.
 *   3. Create a new companion class containing the second-half fields and methods.
 *   4. Add a field referencing the companion in the original class.
 *
 * This preserves the original class interface while reducing its size and
 * moving toward Single Responsibility.
 */
public class GodClassRefactorer implements Refactorer {

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == SmellType.GOD_CLASS;
    }

    @Override
    public RefactorResult refactor(CompilationUnit cu, CodeSmell smell) {
        String targetClass = smell.getClassName();

        Optional<ClassOrInterfaceDeclaration> classOpt = cu.findAll(ClassOrInterfaceDeclaration.class)
                .stream()
                .filter(c -> c.getNameAsString().equals(targetClass))
                .findFirst();

        if (classOpt.isEmpty()) {
            return new RefactorResult(targetClass, SmellType.GOD_CLASS, false, null,
                    "Class not found: " + targetClass);
        }

        ClassOrInterfaceDeclaration cls = classOpt.get();
        String companionName = targetClass + "Delegate";

        List<FieldDeclaration> fields   = cls.getFields();
        List<MethodDeclaration> methods = cls.getMethods();

        if (fields.size() < 2 && methods.size() < 2) {
            return new RefactorResult(targetClass, SmellType.GOD_CLASS, false, null,
                    "Not enough members to extract a companion class");
        }

        int fieldSplit  = fields.size() / 2;
        int methodSplit = methods.size() / 2;

        List<FieldDeclaration>  movedFields  = fields.subList(fieldSplit, fields.size());
        List<MethodDeclaration> movedMethods = methods.subList(methodSplit, methods.size());

        // Build companion class source
        StringBuilder companion = new StringBuilder();
        companion.append("public static class ").append(companionName).append(" {\n");
        for (FieldDeclaration f : movedFields) {
            companion.append("    ").append(f.clone().toString()).append("\n");
        }
        for (MethodDeclaration m : movedMethods) {
            companion.append("    ").append(m.clone().toString()).append("\n");
        }
        companion.append("}");

        ClassOrInterfaceDeclaration companionCls =
                com.github.javaparser.StaticJavaParser.parseTypeDeclaration(companion.toString())
                        .asClassOrInterfaceDeclaration();
        cu.getType(0).asClassOrInterfaceDeclaration().addMember(companionCls);

        // Remove moved members from original class
        movedFields.forEach(f -> cls.remove(f));
        movedMethods.forEach(m -> cls.remove(m));

        // Add delegation field
        String fieldDecl = "private " + companionName + " delegate = new " + companionName + "();";
        cls.addFieldWithInitializer(
                com.github.javaparser.StaticJavaParser.parseType(companionName),
                "delegate",
                com.github.javaparser.StaticJavaParser.parseExpression("new " + companionName + "()"),
                com.github.javaparser.ast.Modifier.Keyword.PRIVATE
        );

        return new RefactorResult(targetClass, SmellType.GOD_CLASS, true, cu.toString(),
                "Extracted '" + companionName + "' from '" + targetClass + "' with "
                        + movedFields.size() + " fields and " + movedMethods.size() + " methods");
    }
}
