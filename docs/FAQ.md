# Frequently Asked Questions

### 1. If a scenario is reachable, won't SonarQube or JaCoCo branch coverage flag it as missing?
Not always. Standard coverage tools track *infrastructure* (did a line of code execute?), not *data combinations*. 
SonarQube will report 100% coverage in two dangerous situations:
* **Shared Exception Pathways:** If Dependency A and Dependency B share a `catch` block (or both throw a generic `RestClientException`), testing Dependency A's failure marks the `catch` block as 100% covered. You remain blind to Dependency B's failure state.
* **Data Flow Without Branches:** If Dependency A returns a boolean and Dependency B returns an Enum, and they interact mathematically or logically further down the method without explicit `if/else` checks, SonarQube sees no branches to miss. ScenarioLens tracks these overlapping data states.

### 2. Can't we just use mutation testing (PITest) to catch these gaps?
Yes, mutation testing is the gold standard for this, but it suffers from two major drawbacks:
* **Execution Time:** Mutation testing executes your test suite hundreds of times. A 3-minute test suite can turn into a 45-minute CI/CD bottleneck. ScenarioLens uses pure static analysis (AST parsing) and runs in under 750ms.
* **Diagnostic vs. Prescriptive:** When a mutation test fails, it tells you a mutant "survived" on line 42, leaving you to reverse-engineer the missing state. ScenarioLens tells you exactly which dependency mock combinations are missing in plain English (or JSON).

### 3. Won't this create a combinatorial explosion of impossible tests?
Blind combinatorial testing does, but ScenarioLens does not. It builds a Control Flow Graph (CFG) of your method and automatically **prunes physically impossible scenarios**. 
For example, if your code checks an inventory dependency and returns early if the item is `OUT_OF_STOCK`, ScenarioLens knows that a downstream payment dependency will never be called. It deletes the `OUT_OF_STOCK` + `PAYMENT_TIMEOUT` scenario from the matrix, keeping your gap report focused only on realistic, reachable states.

### 4. Doesn't this encourage writing too many mocks? Shouldn't these be End-to-End (E2E) tests?
We strongly agree that E2E tests are the ultimate safety net, and over-mocking is an anti-pattern. 
However, forcing a third-party payment gateway to timeout *exactly* when an inventory service returns a specific edge-case state is notoriously difficult and flaky in automated E2E environments. ScenarioLens does not exist to encourage *more* mocking; it audits the unit/integration tests your team is *already writing*. It shifts the discovery of overlapping failure states to the developer's local machine, catching gaps in milliseconds rather than waiting for an E2E pipeline to fail.

### 5. Isn't the shared exception problem solved by just writing separate `catch` blocks for different exceptions?
Yes, if the exceptions are of distinctly different types, you should catch them separately. However, the blind spots ScenarioLens catches extend beyond simple `try/catch` blocks. The tool is most valuable when dependencies return different data states (like `null`, Booleans, or Enums) that interact implicitly further down the method, or when multiple external HTTP clients throw the same generic framework exception.

### 6. What are the limitations? What breaks ScenarioLens?
Because ScenarioLens relies entirely on static analysis (reading the code's structure rather than running it in a JVM), it has specific blind spots:
* **Heavy AOP and Reflection:** It cannot easily trace routing changes dictated by complex Aspect-Oriented Programming, dynamic proxies, or runtime reflection.
* **Business Logic Impossibilities:** It prunes mathematically impossible paths, but it does not know your business rules. If a user status is `NEW`, business logic might dictate their balance is always `0`. The tool might flag `NEW` + `HIGH_BALANCE` as an untested scenario, requiring the developer to manually acknowledge and dismiss the warning.

### 7. Why is my DSC score 0%? I don't use Mockito!
If your repository heavily relies on concrete dummy implementations, state-based fakes (e.g., `Spring Retry`, `Spring Cloud OpenFeign`), or in-memory databases instead of Mockito, ScenarioLens will currently report 0% DSC. Phase 1 exclusively analyzes Mockito (`when/thenReturn`) stubs. Support for state-based fakes and test doubles is actively planned for [Phase 5](ROADMAP.md#phase-5--concrete-fake--dummy-resolution) to ensure teams using these testing best practices are accurately scored.

### 8. Does ScenarioLens force me to write 50 separate test methods for 50 scenarios?
Not at all. ScenarioLens analyzes your test classes holistically. You can satisfy 50 scenarios using a single `@ParameterizedTest` with 50 inputs, or by stubbing multiple behaviors within a few well-structured tests. The tool intelligently merges stubs from `@BeforeEach` and individual `@Test` methods, caring only that the *combinations* of dependency outputs are exercised somewhere in the test suite.

### 9. What if I handle exceptions globally using `@ControllerAdvice` instead of `try/catch` inside the method?
ScenarioLens builds a Control Flow Graph (CFG) of the *method under test*. If a dependency throws an exception and your method propagates it upwards (without catching it), that constitutes a distinct exit path in the CFG. To satisfy this scenario, you simply need a unit test that mocks the dependency to throw the exception, and asserts that your method also throws it (e.g., using `assertThrows(...)`).
