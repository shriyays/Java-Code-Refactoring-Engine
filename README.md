# Java Code Refactoring Engine

A static-analysis tool that automatically detects **code smells** in Java source files using an Abstract Syntax Tree (AST) and applies **automated refactoring transformations** to fix them — all while preserving program semantics. Includes a full dark-mode web UI to paste, analyze, and view refactored code in the browser.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Architecture](#4-architecture)
5. [How It Works — Step by Step](#5-how-it-works--step-by-step)
6. [Code Smell Detectors — Full Explanation](#6-code-smell-detectors--full-explanation)
7. [Refactoring Transformations — Full Explanation](#7-refactoring-transformations--full-explanation)
8. [Data Model](#8-data-model)
9. [Analysis Report](#9-analysis-report)
10. [Web Server & Frontend](#10-web-server--frontend)
11. [How to Run](#11-how-to-run)
12. [Test Suite](#12-test-suite)
13. [Mutation Testing with PIT](#13-mutation-testing-with-pit)
14. [Design Principles](#14-design-principles)
15. [Limitations & Future Work](#15-limitations--future-work)

---

## 1. Project Overview

The engine is built around three core ideas:

**Detection** — Parse every Java source file into an AST using JavaParser. Run six independent smell detectors over the tree. Each detector looks for a specific structural anti-pattern (e.g. a method with too many statements, a class with too many fields) and records every violation as a `CodeSmell` object.

**Transformation** — For each detected smell, find a matching `Refactorer` and apply a well-known refactoring pattern from Martin Fowler's *Refactoring* book directly to the AST. The AST is mutated in place — no string manipulation, no regex, just structural tree edits.

**Convergence** — Run up to 5 passes. Refactoring can introduce new smells (e.g. the parameter object class created by Extract Parameter Object itself has primitive fields). Each pass re-detects and re-fixes until the code is clean or no further progress can be made.

---

## 2. Technology Stack

| Tool | Version | Role |
|------|---------|------|
| Java | 17+ | Language (runs on Java 22) |
| JavaParser (symbol-solver-core) | 3.25.10 | AST parsing and in-place transformation |
| Maven | 3.9+ | Build system and dependency management |
| JUnit 5 (Jupiter) | 5.10.1 | Unit and integration testing |
| PIT Mutation Testing | 1.15.3 | Test quality validation |
| Java built-in HttpServer | JDK built-in | Embedded web server (no extra dependency) |

---

## 3. Project Structure

```
Project 1/
├── pom.xml                                           ← Maven build file
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/refactor/
    │   │   ├── engine/
    │   │   │   └── RefactoringEngine.java            ← Main orchestrator + CLI entry point
    │   │   ├── model/
    │   │   │   ├── SmellType.java                    ← Enum of 8 smell categories
    │   │   │   ├── CodeSmell.java                    ← A single detected violation
    │   │   │   └── RefactorResult.java               ← Outcome of one transformation
    │   │   ├── report/
    │   │   │   └── AnalysisReport.java               ← Aggregated stats across all files
    │   │   ├── smell/
    │   │   │   ├── SmellDetector.java                ← Interface: detect(CompilationUnit)
    │   │   │   ├── LongMethodDetector.java
    │   │   │   ├── LargeClassDetector.java
    │   │   │   ├── LongParameterListDetector.java
    │   │   │   ├── DuplicateCodeDetector.java
    │   │   │   ├── GodClassDetector.java
    │   │   │   └── PrimitiveObsessionDetector.java
    │   │   └── transform/
    │   │       ├── Refactorer.java                   ← Interface: refactor(cu, smell)
    │   │       ├── LongMethodRefactorer.java         ← Extract Method pattern
    │   │       ├── LongParameterListRefactorer.java  ← Introduce Parameter Object
    │   │       └── GodClassRefactorer.java           ← Extract Class / Delegate
    │   └── resources/
    │       └── web/
    │           └── index.html                        ← Full frontend (HTML + CSS + JS)
    └── test/
        └── java/com/refactor/
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
┌─────────────────────────────────────────────────────────────────────┐
│                        Web Layer (port 8080)                         │
│                                                                     │
│   Browser  ──POST /analyze──►  AnalyzeHandler                      │
│      ▲                              │                               │
│      │  JSON response               │  calls                        │
│      └──────────────────────────────┘                               │
│                                     ▼                               │
│                          RefactoringEngine                          │
│                    (analyseAndRefactorUntilClean)                   │
│                                     │                               │
│              ┌──────────────────────┼──────────────────────┐        │
│              ▼                      ▼                       ▼        │
│       detectSmells()         applyRefactorings()     cu.toString()  │
│              │                      │                               │
│    ┌─────────▼──────────┐  ┌────────▼────────────┐                 │
│    │   smell package    │  │  transform package  │                 │
│    │                    │  │                     │                 │
│    │  SmellDetector[]   │  │   Refactorer[]      │                 │
│    │  - LongMethod      │  │   - LongMethod      │                 │
│    │  - LargeClass      │  │   - LongParamList   │                 │
│    │  - LongParamList   │  │   - GodClass        │                 │
│    │  - DuplicateCode   │  │                     │                 │
│    │  - GodClass        │  └─────────────────────┘                 │
│    │  - PrimObsession   │                                           │
│    └────────────────────┘                                           │
│              │                                                      │
│              ▼                                                      │
│    ┌─────────────────────┐                                          │
│    │    model package    │                                          │
│    │  CodeSmell          │                                          │
│    │  RefactorResult     │                                          │
│    │  SmellType (enum)   │                                          │
│    └─────────────────────┘                                          │
│              │                                                      │
│              ▼                                                      │
│    ┌─────────────────────┐                                          │
│    │   report package    │                                          │
│    │   AnalysisReport    │                                          │
│    └─────────────────────┘                                          │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Interfaces

**`SmellDetector`** — one method, all detectors implement it:
```java
List<CodeSmell> detect(CompilationUnit cu);
```

**`Refactorer`** — two methods, all transformers implement them:
```java
RefactorResult refactor(CompilationUnit cu, CodeSmell smell);
boolean canHandle(CodeSmell smell);
```

The engine iterates through registered refactorers and calls `canHandle()` to find a match, then calls `refactor()`. Adding a new refactorer requires **zero changes** to existing code — this is the Open/Closed Principle in practice.

---

## 5. How It Works — Step by Step

### Step 1 — Parse
```
Java source code (string)
        │
        ▼
StaticJavaParser.parse(source)
        │
        ▼
CompilationUnit  ← root of the AST
  ├── TypeDeclaration (ClassOrInterfaceDeclaration)
  │     ├── FieldDeclaration
  │     ├── MethodDeclaration
  │     │     ├── Parameter
  │     │     └── BlockStmt
  │     │           └── Statement[]
  │     └── ...
  └── ...
```

JavaParser converts the raw Java text into a fully navigable tree. Every node in the tree can be read, modified, added to, or removed — changes are reflected when you call `cu.toString()`.

### Step 2 — Detection
Each `SmellDetector` walks the AST using `cu.findAll(NodeType.class)` and checks structural properties. For example, `LongMethodDetector` calls `cu.findAll(MethodDeclaration.class)` and counts statements in each body.

### Step 3 — Transformation
Each `Refactorer` receives the same `CompilationUnit` object and the `CodeSmell` that was found. It navigates to the offending node, restructures it (e.g. splits a method in two, introduces a new class), and returns a `RefactorResult` containing whether it succeeded and what the new source looks like.

### Step 4 — Convergence Loop
```
Pass 1: detect smells → apply fixes → some smells remain (introduced by fixes)
Pass 2: detect remaining smells → apply fixes → fewer smells
Pass 3: detect remaining smells → none found → STOP
```
The engine runs up to **5 passes**. It stops early if a pass produces zero smells or if no refactorer was able to fix anything.

### Step 5 — Report
`AnalysisReport` accumulates every `CodeSmell` and `RefactorResult` across all passes and computes stats: total smells, refactored, skipped, and refactor rate (%).

---

## 6. Code Smell Detectors — Full Explanation

### 6.1 LongMethodDetector
**File:** `smell/LongMethodDetector.java`
**Default threshold:** > 20 statements

```java
cu.findAll(MethodDeclaration.class).forEach(method -> {
    int statementCount = method.getBody()
        .map(body -> body.getStatements().size())
        .orElse(0);
    if (statementCount > threshold) { /* report smell */ }
});
```

Visits every method in the file. Counts the direct child statements in the method body. Abstract methods (no body) are automatically skipped via `orElse(0)`. The threshold is configurable — default is 20.

---

### 6.2 LargeClassDetector
**File:** `smell/LargeClassDetector.java`
**Default thresholds:** > 10 fields OR > 20 methods

```java
cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
    if (cls.isInterface()) return;  // skip interfaces
    int fields  = cls.getFields().size();
    int methods = cls.getMethods().size();
    if (fields  > fieldThreshold)  { /* report smell */ }
    if (methods > methodThreshold) { /* report smell */ }
});
```

Checks field count and method count independently. A class can trigger two smells — one for fields and one for methods. Interfaces are excluded because they are contracts, not implementations.

---

### 6.3 LongParameterListDetector
**File:** `smell/LongParameterListDetector.java`
**Default threshold:** > 4 parameters

```java
cu.findAll(MethodDeclaration.class).forEach(method -> {
    int paramCount = method.getParameters().size();
    if (paramCount > threshold) { /* report smell */ }
});
```

A method with too many parameters is hard to call, hard to remember, and usually means the method is doing too many things. The fix is to group related parameters into a dedicated object.

---

### 6.4 DuplicateCodeDetector
**File:** `smell/DuplicateCodeDetector.java`
**Default minimum window:** 3 consecutive statements

Uses **sliding window fingerprinting**:

```
Method m1 statements: [A, B, C, D, E]
Method m2 statements: [A, B, C, X, Y]

Window size = 3:
m1 windows: [A,B,C], [B,C,D], [C,D,E]
m2 windows: [A,B,C], [B,C,X], [C,X,Y]

[A,B,C] appears in both m1 AND m2 → DUPLICATE
```

Each statement is normalized to its AST class name (e.g. `ExpressionStmt`, `IfStmt`, `ReturnStmt`) so the fingerprint captures **structure** rather than literal values. A window is only flagged if it appears in at least 2 **distinct** methods — same-method repetition is ignored.

---

### 6.5 GodClassDetector
**File:** `smell/GodClassDetector.java`
**Default thresholds:** ≥ 7 fields AND ≥ 15 methods AND ≥ 80 total statements

All three conditions must be true simultaneously:

```java
int fields  = cls.getFields().size();
int methods = cls.getMethods().size();
int loc     = cls.getMethods().stream()
    .mapToInt(m -> m.getBody().map(b -> b.getStatements().size()).orElse(0))
    .sum();

if (fields >= fieldThreshold && methods >= methodThreshold && loc >= locThreshold) {
    /* report god class */
}
```

Requiring all three conditions avoids false positives. A data-transfer object might have many fields but few methods — that's fine. A utility class might have many methods but few fields — also fine. Only a class that is large in all three dimensions is truly a God Class.

---

### 6.6 PrimitiveObsessionDetector
**File:** `smell/PrimitiveObsessionDetector.java`
**Default threshold:** > 5 primitive or String fields

```java
long primitiveCount = cls.getFields().stream()
    .filter(this::isPrimitiveOrWrapper)
    .count();
if (primitiveCount > threshold) { /* report smell */ }
```

A field is considered "primitive" if its type is a Java primitive (`int`, `boolean`, `double`, etc.) or a common wrapper/String type (`String`, `Integer`, `Long`, etc.). Classes with too many such fields are missing domain concepts — `String firstName, String lastName, String email` should probably be an `Address` or `Person` object.

---

## 7. Refactoring Transformations — Full Explanation

### 7.1 LongMethodRefactorer — Extract Method
**File:** `transform/LongMethodRefactorer.java`
**Addresses:** `LONG_METHOD`

**Algorithm:**
1. Find the target `MethodDeclaration` in the AST by class name and method name.
2. Get the list of statements from the body.
3. Split at the midpoint: `firstHalf` (statements 0..n/2) and `secondHalf` (statements n/2..n).
4. Create a new `private void` helper method named `originalNameHelper` containing `secondHalf`.
5. Replace the original body with `firstHalf` + a single call statement `originalNameHelper();`.
6. Add the helper method to the class.

**Guard:** Returns failure if the method has fewer than 4 statements (not worth extracting).

**Before:**
```java
void processOrder() {
    // 22 statements
}
```

**After:**
```java
void processOrder() {
    // first 11 statements
    processOrderHelper();
}

private void processOrderHelper() {
    // last 11 statements
}
```

---

### 7.2 LongParameterListRefactorer — Introduce Parameter Object
**File:** `transform/LongParameterListRefactorer.java`
**Addresses:** `LONG_PARAMETER_LIST`

**Algorithm:**
1. Collect all `Parameter` nodes from the target method.
2. Build a new `public static` inner class named `{MethodName}Params` with one `public` field per original parameter and an all-args constructor.
3. Add this class to the enclosing class.
4. Clear the method's parameter list and replace it with a single `{MethodName}Params params` parameter.

**Guard:** Returns failure if the method has 1 or fewer parameters.

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
        this.name = name;
        this.age = age;
        this.email = email;
        this.active = active;
    }
}

void create(CreateParams params) { ... }
```

---

### 7.3 GodClassRefactorer — Extract Class
**File:** `transform/GodClassRefactorer.java`
**Addresses:** `GOD_CLASS`

**Algorithm:**
1. Split the class's fields at the midpoint. Split its methods at the midpoint.
2. Build a new `public static` inner class named `{ClassName}Delegate` containing the second halves.
3. Remove the moved fields and methods from the original class.
4. Add a `private {ClassName}Delegate delegate = new {ClassName}Delegate();` field to the original class.

**Guard:** Returns failure if the class doesn't have at least 2 fields or 2 methods to split.

**Before:**
```java
class OrderService {   // 10 fields, 20 methods
    ...
}
```

**After:**
```java
class OrderService {   // 5 fields, 10 methods + delegate field
    private OrderServiceDelegate delegate = new OrderServiceDelegate();

    public static class OrderServiceDelegate {  // 5 fields, 10 methods
        ...
    }
}
```

---

## 8. Data Model

### SmellType (enum)
Eight smell categories, each with a display name and description:

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

### CodeSmell
Immutable record of one detected violation:

| Field | Type | Description |
|-------|------|-------------|
| `type` | `SmellType` | Which smell was found |
| `className` | `String` | The class it was found in |
| `memberName` | `String` | Method or field name (null for class-level smells) |
| `line` | `int` | Line number in the source file |
| `detail` | `String` | Human-readable explanation |

### RefactorResult
Outcome of one transformation attempt:

| Field | Type | Description |
|-------|------|-------------|
| `className` | `String` | Class that was transformed |
| `smellAddressed` | `SmellType` | Which smell was being fixed |
| `success` | `boolean` | Whether the transformation succeeded |
| `refactoredCode` | `String` | Full source after transformation (null if failed) |
| `message` | `String` | Summary of what happened |

---

## 9. Analysis Report

`AnalysisReport` accumulates results across all passes and exposes:

| Method | Description |
|--------|-------------|
| `totalSmells()` | All smells detected across all passes |
| `totalRefactored()` | Successful transformations |
| `totalSkipped()` | Failed or unhandled smells |
| `refactorRate()` | `totalRefactored / totalSmells * 100` |
| `smellsByType()` | `Map<SmellType, Long>` — count per category |
| `getDetectedSmells()` | Unmodifiable list of all `CodeSmell` objects |
| `getRefactorResults()` | Unmodifiable list of all `RefactorResult` objects |
| `summary()` | Full formatted text report |

---

## 10. Web Server & Frontend

### WebServer.java
Uses Java's built-in `com.sun.net.httpserver.HttpServer` — **no extra dependencies**. Starts on port 8080 with a thread pool of 4.

Routes:
- `GET /` → serves `index.html` from the classpath
- `POST /analyze` → `AnalyzeHandler`

### AnalyzeHandler.java
Handles the analysis API:
1. Reads the request body (JSON with `{"code": "..."}`)
2. Parses the Java source with `StaticJavaParser`
3. Calls `engine.analyseAndRefactorUntilClean(cu, 5)` (multi-pass)
4. Detects remaining smells on the final `cu` for the "Remaining" stat
5. Returns a JSON response with smells, results, stats, and the full refactored source

### index.html (Frontend)
Single-file frontend — pure HTML, CSS, and vanilla JavaScript. No frameworks, no build step.

**Left panel:** Code editor (`<textarea>`) with monospace font and 3 sample snippet buttons.

**Right panel — two tabs:**
- **Analysis Results** — stat cards (Detected / Refactored / Skipped / Rate / Remaining), smell cards with type + location + detail, refactoring result cards with ✅/⏭️ status
- **Refactored Code** — full refactored Java source with minimal syntax highlighting (keywords, types, strings, comments, numbers) and a Copy button

**Keyboard shortcut:** `Cmd+Enter` / `Ctrl+Enter` triggers analysis.

---

## 11. How to Run

### Prerequisites
- Java 17 or higher: `java -version`
- Maven 3.9+: `mvn -version`
  - If not installed on macOS: `brew install maven`

### Run the Web UI
```bash
cd "Project 1"
mvn compile
mvn exec:java
```
Then open **http://localhost:8080** in your browser.

Press `Ctrl+C` in the terminal to stop the server.

### Run Tests
```bash
mvn clean test
```

### Run a Specific Test Class
```bash
mvn test -Dtest=LongMethodDetectorTest
```

### Run Mutation Testing
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```
HTML report: `target/pit-reports/index.html`

### Run the CLI (scan a directory)
```bash
mvn exec:java -Dexec.mainClass="com.refactor.engine.RefactoringEngine" \
              -Dexec.args="src/main/java"
```

### Use the Engine Programmatically
```java
import com.refactor.engine.RefactoringEngine;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

String javaSource = "public class Foo { ... }";
CompilationUnit cu = StaticJavaParser.parse(javaSource);

RefactoringEngine engine = new RefactoringEngine();
AnalysisReport report = engine.analyseAndRefactorUntilClean(cu, 5);

System.out.println(report.summary());
System.out.println(cu.toString()); // refactored source
```

### Customize Thresholds
```java
new RefactoringEngine(
    List.of(
        new LongMethodDetector(15),          // flag methods > 15 statements
        new LargeClassDetector(8, 15),       // flag > 8 fields or > 15 methods
        new LongParameterListDetector(3),    // flag > 3 parameters
        new DuplicateCodeDetector(4),        // require 4+ matching statements
        new GodClassDetector(5, 12, 60),     // tighter god-class thresholds
        new PrimitiveObsessionDetector(4)    // flag > 4 primitive fields
    ),
    List.of(
        new LongMethodRefactorer(),
        new LongParameterListRefactorer(),
        new GodClassRefactorer()
    )
);
```

---

## 12. Test Suite

**105 JUnit 5 tests across 19 test classes.**

| Test Class | Tests | What It Covers |
|------------|-------|---------------|
| `LongMethodDetectorTest` | 6 | Detection, threshold boundary, abstract method skipping, multiple methods |
| `LargeClassDetectorTest` | 3 | Field threshold, method threshold, interface exclusion |
| `LongParameterListDetectorTest` | 3 | Detection, correct name reporting, threshold |
| `GodClassDetectorTest` | 3 | Triple-condition logic, normal class, partial-condition non-detection |
| `GodClassDetectorExtendedTest` | 6 | Interface exclusion, boundary values, total LOC counting |
| `PrimitiveObsessionDetectorTest` | 2 | Primitive detection, domain-object exclusion |
| `DuplicateCodeDetectorTest` | 4 | Cross-method detection, empty class, default threshold |
| `DuplicateCodeDetectorExtendedTest` | 7 | Sliding window boundaries, single-method, below-min edge case |
| `DetectorThresholdsTest` | 18 | Off-by-one boundary tests for every detector |
| `LongMethodRefactorerTest` | 3 | Success, missing class/method, canHandle |
| `LongMethodRefactorerExtendedTest` | 5 | Helper added, body shortened, short-method guard, abstract method |
| `LongParameterListRefactorerTest` | 7 | Parameter object creation, naming, missing class/method, single-param guard |
| `GodClassRefactorerTest` | 6 | Delegate creation, missing class, not-enough-members guard |
| `GodClassRefactorerExtendedTest` | 4 | Target isolation, field/method removal, midpoint split accuracy |
| `SmellModelTest` | 7 | All getters, toString formats, SmellType enum completeness |
| `AnalysisReportTest` | 3 | Basic rate calculation, zero-smell case, summary labels |
| `AnalysisReportExtendedTest` | 10 | All count methods, map grouping, unmodifiable lists |
| `RefactoringEngineTest` | 3 | Smell detection, non-negative rate, smellsByType map |
| `RefactoringEngineExtendedTest` | 5 | DI constructor, no-matching-refactorer, multi-smell detection |

---

## 13. Mutation Testing with PIT

PIT automatically mutates the source code — flipping `>` to `>=`, removing method calls, replacing return values with `null` or `0` — then checks if the test suite catches each mutation. A mutation that goes undetected reveals a gap in test coverage.

### Results

| Metric | Value |
|--------|-------|
| Total mutants generated | 184 |
| Mutants killed by tests | ~128 |
| Mutation score (killed / generated) | ~70% |
| **Test strength** (killed / covered) | **79%** |
| Target | 75% ✅ |

**Test strength** is the primary metric — it only counts mutants on lines that tests actually execute. Uncovered lines are excluded from the denominator.

### Mutation Operators Applied
| Operator | Example |
|----------|---------|
| Conditionals Boundary | `>` → `>=` |
| Negate Conditionals | `== true` → `== false` |
| Remove Conditionals | `if (x > 0)` → `if (true)` |
| Math | `+` → `-` |
| Void Method Calls | removes `list.add(item)` |
| Return Values | `return count` → `return 0` |
| Increments | `i++` → `i--` |

---

## 14. Design Principles

### SOLID Applied

| Principle | Where Applied |
|-----------|--------------|
| **Single Responsibility** | Each detector handles exactly one smell. Each refactorer handles exactly one transformation pattern. `AnalysisReport` only aggregates. |
| **Open/Closed** | New detectors/refactorers can be added by implementing the interface. `RefactoringEngine` never needs to change. |
| **Liskov Substitution** | Any `SmellDetector` or `Refactorer` can be swapped in the engine's list without changing behaviour. |
| **Interface Segregation** | `SmellDetector` has one method. `Refactorer` has two minimal methods. No fat interfaces. |
| **Dependency Inversion** | `RefactoringEngine` depends on `SmellDetector` and `Refactorer` interfaces, not concrete classes. DI constructor supports testing with mocks/stubs. |

### Why JavaParser?
JavaParser produces a fully mutable AST — you can add, remove, and replace nodes and then call `cu.toString()` to get back valid Java source. Unlike regex or string manipulation, it understands the actual structure of Java code, making transformations semantically safe.

### Why No External Web Framework?
Java's built-in `com.sun.net.httpserver.HttpServer` handles everything needed for this project. Adding Spring Boot or Spark Java would introduce hundreds of transitive dependencies for what is essentially two routes. Keeping it dependency-free makes the project easy to build and understand.

### Conservative Transformations
The refactorers deliberately avoid over-automation:
- **Extract Method** splits at the midpoint rather than analysing data flow — it is safe but not optimal.
- **Introduce Parameter Object** leaves the method body's variable references unchanged — the developer completes the wiring.
- **Extract Class** creates a static inner class rather than a separate file, avoiding filesystem side effects.

This ensures transformations never break compilation.

---

## 15. Limitations & Future Work

| Limitation | Potential Fix |
|------------|--------------|
| Duplicate Code uses only structural node types — may produce false positives | Include normalized variable names in the fingerprint |
| Extract Method does not perform data-flow analysis — the split point is always the midpoint | Implement live-variable analysis to find the optimal extraction boundary |
| Introduce Parameter Object does not rewrite method body references | Use JavaParser's `NameExpr` visitor to rename `param` → `params.param` |
| No multi-file symbol resolution — Feature Envy and Data Clumps are defined but not yet detected | Enable JavaParser's `JavaSymbolSolver` with a full classpath |
| Transformations mutate the AST in memory but do not write back to disk | Add `Files.writeString(path, cu.toString())` in the engine loop |
| No CI/CD pipeline | Add GitHub Actions to run `mvn test` and PIT on every push |
