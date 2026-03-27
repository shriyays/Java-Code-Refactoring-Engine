package com.refactor;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.model.CodeSmell;
import com.refactor.model.SmellType;
import com.refactor.smell.PrimitiveObsessionDetector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrimitiveObsessionDetectorTest {

    // ---- Test 16 ----
    @Test
    void detectsPrimitiveObsession() {
        PrimitiveObsessionDetector detector = new PrimitiveObsessionDetector(3);
        String code = """
                class UserDTO {
                    String firstName;
                    String lastName;
                    int age;
                    String email;
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        List<CodeSmell> smells = detector.detect(cu);
        assertFalse(smells.isEmpty(), "Should detect primitive obsession");
        assertEquals(SmellType.PRIMITIVE_OBSESSION, smells.get(0).getType());
    }

    // ---- Test 17 ----
    @Test
    void doesNotFlagClassWithDomainObjects() {
        PrimitiveObsessionDetector detector = new PrimitiveObsessionDetector(3);
        String code = """
                class Order {
                    Money total;
                    Address shippingAddress;
                    Customer customer;
                }
                """;
        CompilationUnit cu = StaticJavaParser.parse(code);
        assertTrue(detector.detect(cu).isEmpty(), "Domain objects should not trigger primitive obsession");
    }
}
