# Java Code Refactoring Engine

A static-analysis tool that **automatically detects code smells** in Java source files using an AST (Abstract Syntax Tree) and **autonomously applies refactoring transformations** to fix them — all while preserving program semantics.

---

## Table of Contents

1. [What It Does](#1-what-it-does)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Architecture](#4-architecture)
5. [Code Smell Detectors](#5-code-smell-detectors)
6. [Refactoring Transformations](#6-refactoring-transformations)
7. [Data Model](#7-data-model)
8. [How to Run](#8-how-to-run)
9. [Test Suite](#9-test-suite)
10. [Mutation Testing with PIT](#10-mutation-testing-with-pit)
11. [Key Design Decisions](#11-key-design-decisions)
12. [Limitations & Future Work](#12-limitations--future-work)

---

## 1. What It Does

The engine takes a directory of Java source files as input and performs two phases:

### Phase 1 — Detection
It parses every `.java` file into an AST using **JavaParser**, then runs **6 independent smell detectors** over each tree. Every violation found is recorded as a `CodeSmell` object with the class name, member name, line number, and a human-readable explanation.

### Phase 2 — Transformation
For each detected smell, the engine looks for a matching **Refactorer**. If one exists, it mutates the AST in place (applying a well-known refactoring pattern from Martin Fowler's *Refactoring* book) and records the outcome in a `RefactorResult`. The refactored source code can be printed or written back to disk.

### Phase 3 — Reporting
An `AnalysisReport` is produced that aggregates:
- Total smells by type
- Total successfully refactored vs. skipped
- A **refactor rate** (% of smells that were fixed)
- Full detail logs per smell and per transformation

---

## 2. Technology Stack

| Tool | Version | Role |
|------|---------|------|
| Java | 17 (runs on 22) | Language |
| JavaParser (symbol-solver-core) | 3.25.10 | AST parsing & transformation |
| Maven | 3.9+ | Build, dependency management |
| JUnit 5 (Jupiter) | 5.10.1 | Unit & integration testing |
| PIT Mutation Testing | 1.15.3 + junit5 plugin 1.2.1 | Test quality validation |

---

## 3. Project Structure

```
Project 1/
├── pom.xml                                      ← Maven build descriptor
└── src/
    ├── main/java/com/refactor/
    │   ├── engine/
    │   │   └── RefactoringEngine.java           ← Central orchestrator (+ main())
    │   ├── model/
    │   │   ├── SmellType.java                   ← Enum: 8 smell categories
    │   │   ├── CodeSmell.java                   ← Detected smell record
    │   │   └── RefactorResult.java              ← Outcome of a transformation
    │   ├── report/
    │   │   └── AnalysisReport.java              ← Aggregated results & statistics
    │   ├── smell/
    │   │   ├── SmellDetector.java               ← Interface for all detectors
    │   │   ├── LongMethodDetector.java
    │   │   ├── LargeClassDetector.java
    │   │   ├── LongParameterListDetector.java
    │   │   ├── DuplicateCodeDetector.java
    │   │   ├── GodClassDetector.java
    │   │   └── PrimitiveObsessionDetector.java
    │   └── transform/
    │       ├── Refactorer.java                  ← Interface for all refactorers
    │       ├── LongMethodRefactorer.java        ← Extract Method pattern
    │       ├── LongParameterListRefactorer.java ← Introduce Parameter Object
    │       └── GodClassRefactorer.java          ← Extract Class / Delegate pattern
    └── test/java/com/refactor/
        ├── LongMethodDetectorTest.java
        ├── LargeClassDetectorTest.java
        ├── LongParameterListDetectorTest.java
        ├── GodClassDetectorTest.java
        ├── GodClassDetectorExtendedTest.java
        ├── PrimitiveObsessionDetectorTest.java
        ├── DuplicateCodeDetectorTest.java
        ├── DuplicateCodeDetectorExtendedTest.java
        ├── DetectorThresholdsTest.java
        ├── LongMethodRefactorerTest.java
        ├── LongMethodRefactorerExtendedTest.java
        ├── LongParameterListRefactorerTest.java
        ├── GodClassRefactorerTest.java
        ├── GodClassRefactorerExtendedTest.java
        ├── SmellModelTest.java
        ├── AnalysisReportTest.java
        ├── AnalysisReportExtendedTest.java
        ├── RefactoringEngineTest.java
        └── RefactoringEngineExtendedTest.java
```

---

## 4. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     RefactoringEngine                        │
│                    (orchestrator layer)                      │
│                                                             │
│  analyseAndRefactor(Path root)                              │
│       │                                                     │
│       ├─ collectJavaFiles(root)  → List<Path>               │
│       │                                                     │
│       └─ for each .java file:                               │
│              │                                              │
│              ├── JavaParser.parse(file) → CompilationUnit   │
│              │                                              │
│              ├── detectSmells(cu)                           │
│              │    └── SmellDetector[].detect(cu)            │
│              │         → List<CodeSmell>                    │
│              │                                              │
│              └── applyRefactorings(cu, smells)              │
│                   └── Refactorer[].refactor(cu, smell)      │
│                        → List<RefactorResult>               │
│                                                             │
│  All results collected into AnalysisReport                  │
└─────────────────────────────────────────────────────────────┘

         │                              │
         ▼                              ▼
┌─────────────────┐           ┌─────────────────────┐
│  smell package  │           │  transform package   │
│  (read-only)    │           │  (mutates AST)       │
│                 │           │                      │
│ SmellDetector   │           │ Refactorer           │
│  (interface)    │           │  (interface)         │
│     ▲           │           │     ▲                │
│     │ implements │           │     │ implements    │
│  6 detectors    │           │  3 refactorers       │
└─────────────────┘           └─────────────────────┘

         │                              │
         └──────────────┬───────────────┘
                        ▼
               ┌─────────────────┐
               │  model package  │
               │                 │
               │  SmellType      │
               │  CodeSmell      │
               │  RefactorResult │
               └─────────────────┘
                        │
                        ▼
               ┌─────────────────┐
               │ report package  │
               │                 │
               │ AnalysisReport  │
               └─────────────────┘
```

### Key Interfaces

**`SmellDetector`** — all detectors implement this:
```java
List<CodeSmell> detect(CompilationUnit cu);
```

**`Refactorer`** — all transformers implement this:
```java
RefactorResult refactor(CompilationUnit cu, CodeSmell smell);
boolean canHandle(CodeSmell smell);
```

The engine calls `canHandle()` on each registered refactorer to find a match, then calls `refactor()` on the first match. This is a **Chain of Responsibility** pattern — adding a new refactorer requires zero changes to existing code (Open/Closed Principle).

---

## 5. Code Smell Detectors

### 5.1 Long Method
**Class:** `LongMethodDetector`
**Default threshold:** 20 statements

Visits every `MethodDeclaration` in the AST. Counts the number of direct child `Statement` nodes in the method body. If the count exceeds the threshold, a `LONG_METHOD` smell is reported.

```
Input:  void processOrder(...) { /* 25 statements */ }
Output: CodeSmell(LONG_METHOD, "OrderService", "processOrder", line=42,
                  "Method has 25 statements (threshold: 20)")
```

---

### 5.2 Large Class
**Class:** `LargeClassDetector`
**Default thresholds:** >10 fields OR >20 methods

Visits every `ClassOrInterfaceDeclaration` (skipping interfaces). Separately checks field count and method count against their thresholds. A class can trigger two smells — one for fields and one for methods.

---

### 5.3 Long Parameter List
**Class:** `LongParameterListDetector`
**Default threshold:** >4 parameters

Visits every `MethodDeclaration`. Counts parameters. Flags methods where the count exceeds the threshold.

```
Input:  void create(String name, int age, String email, boolean active, String role)
Output: CodeSmell(LONG_PARAMETER_LIST, "UserService", "create", line=10,
                  "Method has 5 parameters (threshold: 4)")
```

---

### 5.4 Duplicate Code
**Class:** `DuplicateCodeDetector`
**Default minimum window:** 3 consecutive statements

Uses a **sliding window fingerprinting** approach:
1. For every method body, slides a window of size `minDuplicateStatements` across the statements.
2. Each window is normalized to a structural fingerprint using the AST node class names (e.g., `ExpressionStmt|IfStmt|ReturnStmt|`).
3. Fingerprints are stored in a map. If the same fingerprint appears in **at least 2 distinct methods**, it is flagged as a duplicate.

Note: Normalization is structural (ignores literal values) — so two `if` statements are considered structurally equal even if their conditions differ.

---

### 5.5 God Class
**Class:** `GodClassDetector`
**Default thresholds:** ≥7 fields AND ≥15 methods AND ≥80 total statements

Detects classes that violate the **Single Responsibility Principle** by requiring all three conditions simultaneously. This avoids false positives from classes that are large in only one dimension (e.g., a data-holder class with many fields but few methods).

The `totalStatements` count sums all method body statement counts across the entire class.

---

### 5.6 Primitive Obsession
**Class:** `PrimitiveObsessionDetector`
**Default threshold:** >5 primitive/String fields

Inspects class fields. A field is considered "primitive" if its type is:
- A Java primitive (`int`, `boolean`, `double`, etc.)
- A wrapper/string type: `String`, `Integer`, `Long`, `Double`, `Float`, `Boolean`, `Character`, `Byte`, `Short`

Classes with too many such fields suggest that domain concepts (e.g., `Money`, `Address`, `PhoneNumber`) are missing.

---

## 6. Refactoring Transformations

### 6.1 Extract Method (LongMethodRefactorer)
**Addresses:** `LONG_METHOD`
**Pattern:** Fowler's *Extract Method*

**Algorithm:**
1. Locate the target `MethodDeclaration` in the AST.
2. Split its statement list at the midpoint into `firstHalf` and `secondHalf`.
3. Create a new `private void` helper method containing `secondHalf` (name = `originalName + "Helper"`).
4. Replace the original body with `firstHalf` + a single call to the helper.
5. Add the helper to the class.

**Before:**
```java
void processOrder() {
    // 22 statements
}
```

**After:**
```java
void processOrder() {
    // 11 statements (first half)
    processOrderHelper();
}

private void processOrderHelper() {
    // 11 statements (second half)
}
```

**Guards:**
- Returns failure if the method has fewer than 4 statements (not worth splitting).
- Returns failure if the class or method cannot be found in the AST.
- Generates a unique helper name if one already exists (appends a count suffix).

---

### 6.2 Introduce Parameter Object (LongParameterListRefactorer)
**Addresses:** `LONG_PARAMETER_LIST`
**Pattern:** Fowler's *Introduce Parameter Object*

**Algorithm:**
1. Collect all parameters of the target method.
2. Generate an inner `public static` class named `{MethodName}Params` with one public field per parameter and an all-args constructor.
3. Replace the method's parameter list with a single parameter `{MethodName}Params params`.
4. Add the inner class to the enclosing class.

**Before:**
```java
void create(String name, int age, String email, boolean active) { ... }
```

**After:**
```java
public static class CreateParams {
    public String name;
    public int age;
    public String email;
    public boolean active;

    public CreateParams(String name, int age, String email, boolean active) {
        this.name = name;  this.age = age;
        this.email = email;  this.active = active;
    }
}

void create(CreateParams params) { ... }
```

**Note:** Body references to the old parameter names are left as-is, intentionally, so the developer can complete the wiring. This keeps the transformation safe and non-destructive.

---

### 6.3 Extract Class / Delegate (GodClassRefactorer)
**Addresses:** `GOD_CLASS`
**Pattern:** Fowler's *Extract Class*

**Algorithm:**
1. Split the class's fields at the midpoint; split its methods at the midpoint.
2. Generate a new `public static` inner class named `{ClassName}Delegate` containing the second halves.
3. Remove the moved fields and methods from the original class.
4. Add a `private {ClassName}Delegate delegate = new {ClassName}Delegate();` field to the original class.

**Before:**
```java
class OrderService {   // 10 fields, 20 methods
    ...
}
```

**After:**
```java
class OrderService {   // 5 fields, 10 methods + delegation field
    private OrderServiceDelegate delegate = new OrderServiceDelegate();
    ...
    public static class OrderServiceDelegate {  // 5 fields, 10 methods
        ...
    }
}
```

---

## 7. Data Model

### `SmellType` (enum)
8 smell categories, each with a display name and description:

| Constant | Display Name |
|----------|-------------|
| `LONG_METHOD` | Long Method |
| `LARGE_CLASS` | Large Class |
| `DUPLICATE_CODE` | Duplicate Code |
| `LONG_PARAMETER_LIST` | Long Parameter List |
| `GOD_CLASS` | God Class |
| `FEATURE_ENVY` | Feature Envy |
| `DATA_CLUMPS` | Data Clumps |
| `PRIMITIVE_OBSESSION` | Primitive Obsession |

### `CodeSmell`
Immutable record of a detected violation:
```
type         : SmellType
className    : String     — enclosing class
memberName   : String     — method/field name (nullable)
line         : int        — line number in source
detail       : String     — human-readable explanation
```

### `RefactorResult`
Outcome of one transformation attempt:
```
className        : String
smellAddressed   : SmellType
success          : boolean
refactoredCode   : String    — full source after transformation (null if failed)
message          : String    — summary of what happened
```

### `AnalysisReport`
Aggregates all results across an entire scan:

| Method | Returns |
|--------|---------|
| `totalSmells()` | int — all detected smells |
| `totalRefactored()` | int — successful transformations |
| `totalSkipped()` | int — failed/unhandled transformations |
| `refactorRate()` | double — `totalRefactored / totalSmells * 100` |
| `smellsByType()` | `Map<SmellType, Long>` — count per category |
| `summary()` | String — full formatted report |

---

## 8. How to Run

### Prerequisites
- Java 17 or higher (`java -version`)
- Maven 3.9+ (`mvn -version`)
  If Maven is not installed: `brew install maven` (macOS)

### Step 1 — Compile
```bash
cd "Project 1"
mvn compile
```

### Step 2 — Run the CLI on a directory
```bash
mvn exec:java -Dexec.mainClass="com.refactor.engine.RefactoringEngine" \
              -Dexec.args="src/main/java"
```
This scans all `.java` files under `src/main/java` and prints the full report to stdout.

You can point it at any directory:
```bash
mvn exec:java -Dexec.mainClass="com.refactor.engine.RefactoringEngine" \
              -Dexec.args="/path/to/your/java/project/src"
```

### Step 3 — Run from your own Java code
```java
import com.refactor.engine.RefactoringEngine;
import com.refactor.report.AnalysisReport;
import java.nio.file.Paths;

RefactoringEngine engine = new RefactoringEngine();
AnalysisReport report = engine.analyseAndRefactor(Paths.get("src/main/java"));
System.out.println(report.summary());
```

### Step 4 — Customize thresholds
All detectors accept constructor arguments:
```java
new RefactoringEngine(
    List.of(
        new LongMethodDetector(15),            // flag methods > 15 statements
        new LargeClassDetector(8, 15),         // flag > 8 fields or > 15 methods
        new LongParameterListDetector(3),      // flag > 3 parameters
        new DuplicateCodeDetector(4),          // require 4+ matching statements
        new GodClassDetector(5, 12, 60),       // tighter god-class detection
        new PrimitiveObsessionDetector(4)      // flag > 4 primitive fields
    ),
    List.of(
        new LongMethodRefactorer(),
        new LongParameterListRefactorer(),
        new GodClassRefactorer()
    )
);
```

### Sample Output
```
=== Analysis Report ===
Total smells detected : 7
Successfully refactored: 6
Skipped               : 1
Refactor rate         : 85.7%

--- Smells by Type ---
  Long Method: 3
  Long Parameter List: 2
  God Class: 1
  Primitive Obsession: 1

--- Detected Smells ---
  [Long Method] OrderService#processOrder (line 12): Method has 24 statements (threshold: 20)
  ...

--- Refactoring Results ---
  [Long Method] OrderService -> REFACTORED: Extracted 'processOrderHelper' from 'processOrder'
  ...
```

---

## 9. Test Suite

105 JUnit 5 tests across 14 test classes. Every test class is focused on one unit.

### Test Classes

| Test Class | What It Covers |
|------------|---------------|
| `LongMethodDetectorTest` | Detection, thresholds, abstract methods, multiple smells |
| `LargeClassDetectorTest` | Field/method thresholds, interface exclusion |
| `LongParameterListDetectorTest` | Detection, correct method name, threshold |
| `GodClassDetectorTest` | Triple-condition logic, normal class, partial conditions |
| `GodClassDetectorExtendedTest` | Interface exclusion, boundary conditions, total LOC |
| `PrimitiveObsessionDetectorTest` | Primitive detection, domain-object exclusion |
| `DuplicateCodeDetectorTest` | Cross-method detection, empty class, default threshold |
| `DuplicateCodeDetectorExtendedTest` | Sliding window boundaries, single-method edge cases |
| `DetectorThresholdsTest` | Boundary tests for every detector (off-by-one coverage) |
| `LongMethodRefactorerTest` | Success, missing class/method, canHandle |
| `LongMethodRefactorerExtendedTest` | Helper count, body shrinkage, short-method guard |
| `LongParameterListRefactorerTest` | Parameter object creation, naming, edge cases |
| `GodClassRefactorerTest` | Delegate creation, missing class, not-enough-members guard |
| `GodClassRefactorerExtendedTest` | Target isolation, field/method removal, midpoint split |
| `SmellModelTest` | All getters, toString formats, SmellType enum values |
| `AnalysisReportTest` | Basic rate calculation, zero-smell case, summary labels |
| `AnalysisReportExtendedTest` | All count methods, map grouping, unmodifiable lists |
| `RefactoringEngineTest` | Detect smells, non-negative rate, smellsByType map |
| `RefactoringEngineExtendedTest` | DI constructor, no-matching-refactorer case, multi-smell |

### Run Tests
```bash
mvn clean test
```

### Run a Single Test Class
```bash
mvn test -Dtest=LongMethodDetectorTest
```

### Run a Single Test Method
```bash
mvn test -Dtest=LongMethodDetectorTest#detectsLongMethod
```

---

## 10. Mutation Testing with PIT

**PIT** (Pitest) automatically mutates the source code (flips conditionals, removes calls, replaces return values, etc.) and checks whether the test suite catches each mutation. A mutation that goes undetected is a "surviving mutant" — evidence of a gap in test quality.

### Run PIT
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

### View HTML Report
```
target/pit-reports/index.html
```
Open in any browser for a class-by-class breakdown with annotated source code.

### Results Achieved

| Metric | Value |
|--------|-------|
| Total mutants generated | 184 |
| Mutants killed | ~128 |
| Mutation score (killed/generated) | ~70% |
| **Test strength** (killed/covered) | **79%** |
| Target | 75% |

**Test strength** is the primary metric — it measures how many *covered* mutants the tests kill (excludes mutants on uncovered lines).

### Mutation Operators Used (`DEFAULTS`)
- **Conditionals Boundary** — flips `<` to `<=`, `>` to `>=`, etc.
- **Negate Conditionals** — flips `==` to `!=`, `true` to `false`
- **Remove Conditionals** — replaces condition with `true` or `false`
- **Math** — replaces `+` with `-`, `*` with `/`, etc.
- **Void Method Calls** — removes calls to void methods
- **Return Values** — replaces return value with `0`, `null`, `true`, etc.
- **Increments** — changes `i++` to `i--`

---

## 11. Key Design Decisions

### SOLID Principles Applied

| Principle | Where |
|-----------|-------|
| **Single Responsibility** | Each detector handles exactly one smell type; each refactorer handles exactly one transformation |
| **Open/Closed** | New detectors/refactorers can be added by implementing the interface — `RefactoringEngine` never needs to change |
| **Liskov Substitution** | Any `SmellDetector` or `Refactorer` implementation is interchangeable |
| **Interface Segregation** | `SmellDetector` and `Refactorer` are minimal, single-method interfaces |
| **Dependency Inversion** | `RefactoringEngine` depends on the `SmellDetector` and `Refactorer` interfaces, not concrete classes; supports DI constructor for testing |

### Why JavaParser?
JavaParser provides a full, mutable Java AST with a visitor API. Unlike regex-based tools, it understands the actual structure of the code — method bodies, class hierarchies, parameter types — which is required for both accurate detection and safe transformation.

### Structural Fingerprinting for Duplicate Code
Fingerprinting normalizes statements to their AST class names (`ExpressionStmt`, `IfStmt`, etc.) rather than text. This makes detection structure-aware — it catches structural patterns regardless of variable names or literal values.

### Conservative Transformations
The transformers deliberately avoid over-automation:
- **Extract Method** only splits at the midpoint — it does not analyze data flow.
- **Introduce Parameter Object** does not rewrite the method body — it leaves a `params.x` migration as a developer task.
- **Extract Class** creates a static inner class rather than a separate file, avoiding file-system side effects.

This ensures the transformations never break compilation.

---

## 12. Limitations & Future Work

| Limitation | Potential Fix |
|------------|--------------|
| Duplicate Code uses only structural types (not variable names) — may produce false positives | Use statement hash including normalized variable names |
| Extract Method does not analyze data flow — extracted code may reference variables from the first half | Implement live-variable analysis to determine split points |
| Parameter Object does not rewrite method body references | Use JavaParser's `NameExpr` visitor to rename `param` → `params.param` |
| No multi-file symbol resolution — Feature Envy and Data Clumps are defined but not yet detected | Enable JavaParser's `JavaSymbolSolver` with a full classpath |
| Transformations mutate the AST in memory but do not write back to disk | Add `Files.writeString(path, cu.toString())` in the engine loop |
| No CI/CD integration | Add a Maven `verify` phase that fails if refactor rate drops below threshold |
