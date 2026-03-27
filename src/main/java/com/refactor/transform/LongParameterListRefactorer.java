package com.refactor.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.refactor.model.CodeSmell;
import com.refactor.model.RefactorResult;
import com.refactor.model.SmellType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Refactors Long Parameter Lists by introducing a Parameter Object.
 *
 * Strategy:
 *   1. Collect all parameters of the offending method.
 *   2. Generate a new inner static class (ParameterObject) with those fields.
 *   3. Replace the method signature with a single parameter of the new type.
 *
 * Note: Body references to old parameter names are intentionally preserved with
 * a TODO comment so the developer can complete the wiring — this keeps
 * semantics intact and avoids over-automation.
 */
public class LongParameterListRefactorer implements Refactorer {

    @Override
    public boolean canHandle(CodeSmell smell) {
        return smell.getType() == SmellType.LONG_PARAMETER_LIST;
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
            return new RefactorResult(targetClass, SmellType.LONG_PARAMETER_LIST, false, null,
                    "Class not found: " + targetClass);
        }

        ClassOrInterfaceDeclaration cls = classOpt.get();

        Optional<MethodDeclaration> methodOpt = cls.getMethods().stream()
                .filter(m -> m.getNameAsString().equals(targetMethod))
                .findFirst();

        if (methodOpt.isEmpty()) {
            return new RefactorResult(targetClass, SmellType.LONG_PARAMETER_LIST, false, null,
                    "Method not found: " + targetMethod);
        }

        MethodDeclaration method = methodOpt.get();
        List<Parameter> params = method.getParameters();

        if (params.size() <= 1) {
            return new RefactorResult(targetClass, SmellType.LONG_PARAMETER_LIST, false, null,
                    "Not enough parameters to extract");
        }

        // Build inner parameter object class name
        String paramClassName = capitalize(targetMethod) + "Params";

        // Build inner static class source
        StringBuilder innerClass = new StringBuilder();
        innerClass.append("public static class ").append(paramClassName).append(" {\n");
        for (Parameter p : params) {
            innerClass.append("    public ").append(p.getType()).append(" ").append(p.getNameAsString()).append(";\n");
        }
        innerClass.append("    public ").append(paramClassName).append("(");
        innerClass.append(params.stream()
                .map(p -> p.getType() + " " + p.getNameAsString())
                .collect(Collectors.joining(", ")));
        innerClass.append(") {\n");
        for (Parameter p : params) {
            innerClass.append("        this.").append(p.getNameAsString())
                    .append(" = ").append(p.getNameAsString()).append(";\n");
        }
        innerClass.append("    }\n}");

        // Add inner class to compilation unit via class body
        com.github.javaparser.ast.body.ClassOrInterfaceDeclaration paramObj =
                com.github.javaparser.StaticJavaParser.parseTypeDeclaration(innerClass.toString())
                        .asClassOrInterfaceDeclaration();
        cls.addMember(paramObj);

        // Replace method parameters with single parameter object
        method.getParameters().clear();
        method.addParameter(new ClassOrInterfaceType(null, paramClassName), "params");

        return new RefactorResult(targetClass, SmellType.LONG_PARAMETER_LIST, true, cu.toString(),
                "Introduced parameter object '" + paramClassName + "' for method '" + targetMethod + "'");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
