# ScenarioLens — Roadmap

---

## Criterion

**Dependency Scenario Coverage (DSC)**
A new test adequacy criterion that measures what percentage of the
meaningful input x dependency-response scenario space is exercised
by an existing Mockito-based unit test suite, derived from static
CFG analysis of the method under test.

Positions as the fourth coverage dimension:
```
Line coverage     → JaCoCo
Branch coverage   → SonarQube
Mutation score    → PIT
DSC score         → ScenarioLens
```

---

## Phase 1 — COMPLETE ✅
**Scope: Unit tests + Mockito only. No LLM. Pure static analysis.**

### Delivered
- JavaParser AST extraction with JavaSymbolSolver type resolution
- Control Flow Graph (CFG) construction
  - if/else handling
  - try/catch/finally handling
  - switch statement handling
  - Nested condition handling
- Outgoing call detection and classification
  - Spring Data repositories
  - Injected interfaces
  - KafkaTemplate
  - ApplicationEventPublisher
  - RestTemplate / Feign (basic)
- Return variation enumeration
  - Enum value expansion (all constants)
  - Null return detection (where no null guard exists)
  - Boolean method outcomes (true/false)
  - Exception paths (checked + unchecked)
  - Void method presence/absence per path
- CFG-based path pruning (three rules)
  - Rule 1: Call reachability map from CFG
  - Rule 2: Eliminate combinations where call not reachable on path
  - Rule 3: Stub variation forces path, downstream stubs must be consistent
- Negation scenarios (what must NOT happen on each path)
- Literal and static final constant boundary detection
  - value-1, value, value+1 at each numeric boundary
- Mockito stub extraction from existing test classes
  - when/thenReturn
  - when/thenThrow
  - doReturn
  - @BeforeEach stub merging with per-test overrides
- Assertion strength classification
  - STRONG: assertEquals, assertThrows, verify with argThat/capture
  - WEAK: assertNotNull, assertTrue without value check
- Three-tier gap report
  - AUTO-VALIDATED: tool fully verifies, missing = build failure
  - BOUNDARY: dynamic value, developer confirms
  - INFO: structural limitation, advisory only, never fails build
- @SuppressScenario annotation escape hatch
- Maven plugin (scenariolens-maven-plugin)
  - targetPackages configuration
  - minScenarioCoverage threshold
  - failOnMissing flag
  - failOnBoundary flag (default false)
  - outputDirectory configuration
  - maxScenariosPerMethod cap (default 500)
- Report output formats
  - HTML report (human readable, light/dark theme toggle)
  - JSON report (machine readable, LLM ready)
- Recursive package traversal (Files.walk)
- Field-name filtering to exclude local var calls
- PathPrunerTest gate test (81% pruning on processRefund)

### Stress Test Results (Phase 1)
```
Corpus                    Classes  Methods  Crashes  Notes
PaymentService (examples)       1       22        0  82-86% pruning, includes pathological methods
Spring PetClinic              —       79        0  processCreationForm: 16384→4 (99.97%)
Baeldung Mockito              —       63        0  createMessage: 262144→1 after field filter
Apache Kafka clients         471    3,870        0  combinatorial guard activated on complex producers
Spring Framework txn         129      680        0  handles deep Spring proxy chains
```

### Known Phase 1 Limitations (Addressed in Later Phases)
- No SpringBootTest / @MockBean support → Phase 3
- No WireMock stub extraction → Phase 3
- No config-driven boundary values (@Value, application.properties) → Phase 2
- No @Spy partial mock detection → Phase 3
- No Mockito ArgumentCaptor analysis in stub extraction → Phase 4
- No static method stubs (Mockito.mockStatic) → Phase 3
- No multi-method flow tracing → Phase 4
- No Gradle plugin → Phase 3
- No SonarQube plugin → Phase 3
- No LCOV output → Phase 3
- No numeric boundary for runtime-resolved thresholds → Phase 2
- No Optional<T> full support → Phase 4

---

## AI Agent Workflow — No Code Needed

LLM integration is intentionally not built into ScenarioLens.
The JSON report is already LLM-ready. Any agent can drive the
iteration loop with this single instruction:

> Run the ScenarioLens Maven plugin which generates a coverage
> gap analysis report at target/scenariolens/report.json.
> Review the report and generate missing tests.
> Repeat until DSC score is above 80%.

Works today with Claude, Gemini, GitHub Copilot, or any agent
that can run shell commands and read files. No API keys, no
provider lock-in, no maintenance burden.

---

## Prioritized Next Steps — Least Effort / Highest ROI

Sequenced by community impact per unit of build effort.

```
Priority  Feature                         Effort   ROI      Why
────────────────────────────────────────────────────────────────────────────
1         SonarQube generic XML output    1 day    HIGH     [x] COMPLETED
                                                            DSC score on
                                                            existing dashboard
                                                            engineers check daily

2         LCOV output                     1 day    HIGH     [x] COMPLETED
                                                            GitHub Actions PR
                                                            comments, VS Code
                                                            gutter indicators,
                                                            Codecov integration

3         Extract boundary from           2 days   HIGH     Closes BOUNDARY tier
          existing test Mockito stub                        gaps with zero
          (no execution needed —                            execution — already
          already parsed in Phase 1)                        parsed in Phase 1

4         Read @Value properties files    2 days   HIGH     Resolves most common
          for boundary resolution                           config-driven
                                                            threshold pattern

5         Optional<T> full support        2 days   HIGH     Extremely common
                                                            in Spring codebases
                                                            Silent gap today

6         Gradle plugin                   3 days   MEDIUM   Doubles addressable
                                                            audience — many
                                                            teams use Gradle

7         Exception field precision       1 day    MEDIUM   Easy INFO tier
          validation                                        addition — assert
                                                            errorCode not just
                                                            exception type

8         Full Feign client detection     3 days   MEDIUM   Common in Spring
                                                            microservices

9         WireMock stub extraction        1 week   MEDIUM   Extends tool to
                                                            IT/FT suites

10        Purity gate + pure static       1 week   MEDIUM   Closes remaining
          method boundary resolution                        BOUNDARY tier gaps

11        @MockBean support               1 week   MEDIUM   SpringBootTest
                                                            awareness

12        @BoundarySource annotation      2 days   LOW      Escape hatch for
                                                            complex boundaries

13        Multi-method flow tracing       2 weeks  LOW      High complexity,
                                                            niche use case
                                                            until adoption grows

14        IDE plugins (VS Code,           2 weeks  LOW      Build after LCOV —
          IntelliJ)                                         community may
                                                            contribute these

15        Full SonarQube plugin           2 weeks  LOW      Do after Sonar XML
                                                            proves demand
```

Items 1-2 are the immediate post-release additions.
Both take one day each and put ScenarioLens where engineers already look.

---

## Phase 2 — Hybrid Boundary Resolution
**Goal: Resolve dynamic boundary values without full test execution.**

### Features
- [ ] Purity gate check before any code execution
      Verify: no IO, no DB, no network, no Spring context, no mutable static state
      If any check fails → stays BOUNDARY tier, not executed
- [ ] Execute pure static methods for boundary value resolution
      e.g. CurrencyUtils.toBaseUnit("USD", 1000) → resolves to 100000
- [ ] Read @Value properties files
      Read application.properties, application-test.properties
      Resolve: refund.threshold=1000 → boundary value 1000
- [ ] Extract boundary from existing test Mockito stub
      If test already stubs configService.getThreshold() → 1000
      Use 1000 as boundary value → generate 999, 1000, 1001 scenarios
      This requires no execution — already parsed in Phase 1 test AST
- [ ] @BoundarySource annotation for explicit dynamic boundary declaration
      @BoundarySource(method = "configService.getRefundThreshold")
      Upgrades BOUNDARY tier item to AUTO-VALIDATED
- [ ] Trusted classes whitelist config
      -DtrustedClasses=com.example.constants.*,com.example.utils.CurrencyUtils
      Explicit opt-in for classes where purity gate is conservative
- [ ] Classloader warmup amortization across run
      Single classloader init (~100ms) amortized across all boundary resolutions
      Target: <5% total pipeline time increase in hybrid mode

---

## Phase 3 — Ecosystem Expansion
**Goal: Broader framework support, IDE integration, pipeline integration.**

### Output Formats
- [x] SonarQube generic coverage XML
      sonar.coverageReportPaths=target/scenariolens/sonar-coverage.xml
      Puts DSC score on SonarQube dashboard without full Sonar plugin
      HIGHEST PRIORITY in Phase 3 — fastest path to existing engineer dashboards
- [x] LCOV format output
      Enables GitHub Actions PR coverage comments
      Enables VS Code coverage gutters extension (inline scenario coverage)
      Enables Codecov.io and Coveralls.io integration
      Format: scenariolens.lcov in output directory

### Framework Support
- [ ] WireMock stub extraction
      Parse WireMock stubFor() configurations in IT/FT test classes
      Map HTTP response variations to scenario matrix
      Detect contract drift between stubs and actual API behavior
- [ ] Full Feign client detection
      Resolve Feign interface methods to outgoing HTTP calls
      Map response type variations same as injected interfaces
- [ ] @MockBean support
      Extract stubs from @SpringBootTest + @MockBean test classes
      Partial IT/FT suite awareness
- [ ] @Spy partial mock detection
      Distinguish real method calls from stubbed calls on spy objects
- [ ] Mockito ArgumentCaptor analysis in stub extraction
      Extract expected argument values from captor assertions
      Classify as STRONG assertion with argument content verification
- [ ] Mockito.mockStatic support
      Detect static method stubs
      Add to scenario matrix as outgoing call variations

### Build Tool Support
- [ ] Gradle plugin
      io.scenariolens:scenariolens-gradle-plugin
      Same configuration surface as Maven plugin
      Target: scenariolens { targetPackages = [...] minDscScore = 70 }

### IDE Integration
- [ ] VS Code extension
      Right-click method → Show DSC coverage
      Inline gutter indicators for covered/missing scenarios
      Uses LCOV output from Phase 4
- [ ] IntelliJ IDEA plugin
      Same capability as VS Code extension
      Integrates with IntelliJ's built-in coverage view

### SonarQube Plugin
- [ ] Full SonarQube plugin
      Custom metrics: dsc_score, missing_scenario_count, weak_assertion_count
      Quality gate: fail if dsc_score < threshold
      Scenario detail visible on SonarQube issue view per line
      Publish to SonarQube marketplace
      Note: community sonar-pitest-plugin is unmaintained since SonarQube 8.x
            ScenarioLens plugin targets SonarQube 9.x and 10.x

---

## Phase 4 — Advanced Analysis
**Goal: Multi-method tracing, numeric boundaries, deeper assertion analysis.**

### Features
- [ ] Multi-method flow tracing
      Trace calls that chain across service methods
      e.g. processRefund → validateRequest → checkEligibility
      Build inter-method CFG for scenario matrix generation
- [ ] Numeric boundary generation without annotation
      Detect threshold values from unit test input patterns
      Statistical inference: if tests use 0, 100, 1000 → infer boundaries
- [ ] Assertion causal correctness hints
      Detect when assertion value could pass regardless of logic
      e.g. assertEquals(SUCCESS, response.getStatus()) when method always returns SUCCESS
      Flag as INFO: "verify assertion is causally linked to logic, not coincidental"
- [ ] Exception field precision validation
      Detect when test asserts exception type but not message or error code
      Generate INFO item: assert errorCode and message not just exception class
- [ ] State mutation side effect detection
      Detect void calls that mutate object state before passing to next call
      Generate INFO: verify argument state passed to save(), not just that save() was called
      Suggest ArgumentCaptor assertion pattern
- [ ] Loop body scenario generation
      Unroll one iteration for scenario generation
      Handle early returns inside loops
- [ ] Optional<T> full support
      Model Optional.isPresent(), Optional.get(), Optional.orElseThrow() paths
      Generate scenarios for empty vs present Optional returns

---

## Phase 5 — Concrete Fake & Dummy Resolution

**Goal:** Resolve dependency scenarios from state-based fakes and dummy implementations.

### Features
- [x] **Fake Discovery Mechanism**
      Scan `src/test/java` for Spring `@TestConfiguration` and `@Bean` definitions that return interface implementations (MVP shipped!).
- [x] **State-Mapping Engine (GA)**
      Analyze the conditional return values of overridden methods in these fakes using heuristic symbolic mapping (shipped!).
- [x] **Scenario Matrix Integration**
      Treat a fake's deterministic return values exactly like Mockito stubs to fulfill CFG paths.
- [ ] **Annotation-based Discovery**
      Explore introducing a `@ScenarioFake` annotation for teams not using Spring DI for tests.

---


## Academic and Community Track

### arXiv Paper
- [ ] Write paper (4-6 pages)
      Title: ScenarioLens: Dependency Scenario Coverage for Unit Test Adequacy
             in Dependency-Injected Java Systems
      Sections: Introduction, Background, DSC Criterion (formal),
                Algorithm, Evaluation, Related Work, Conclusion
      Key numbers to include: 99.97% pruning PetClinic, 262144→1 Baeldung
      Related work to cite: MockMill 2026, TestGeneralizer 2026,
                            CloneDeMocker 2025, PIT, JaCoCo, EvoSuite, Diffblue
- [ ] Submit to arXiv cs.SE
- [ ] Link arXiv paper in README

### Maven Central Release
- [x] Set version to 0.1.0 in pom.xml
- [x] Configure GPG signing (required by Maven Central)
- [x] Configure maven-publish plugin with Sonatype credentials
- [x] mvn deploy → release via central.sonatype.com UI
- [x] Verify artifact available at search.maven.org

### Community Launch
- [ ] Post on r/java (tool announcement)
- [ ] Post on r/programming
- [ ] Dev.to blog post (methodology explanation)
- [ ] Submit PR to awesome-java
      github.com/akullpp/awesome-java → Testing section
      Highest-leverage OSS discovery channel for Java
- [ ] Email SonarSource community forum (after 200+ GitHub stars)
- [ ] Target conference talk: Devoxx, JavaOne, ICST 2027

---

## Open Design Decisions

```
1. Kotlin support
   Partial, community-driven — not first class in Phase 1
   Revisit in Phase 4 if community demand exists

2. Language-agnostic expansion
   Name ScenarioLens is language-agnostic by design
   Python (pytest + MagicMock), .NET (NUnit + Moq) possible future targets
   JavaParser is Java-specific — would need separate parser per language

3. Incremental analysis
   Only analyze changed classes between runs
   Requires caching scenario matrix per class + change detection
   Phase 5 consideration
```

---

## Competitive Landscape

| Tool | What it does | Gap ScenarioLens fills |
|------|-------------|----------------------|
| JaCoCo | Line coverage | No scenario awareness |
| SonarQube | Branch coverage | No stub combination modeling |
| PIT | Mutation testing | No missing scenario detection, 20-50x slower |
| MockMill 2026 | Extracts Mockito stubs for LLM generation | No matrix, no pruning, no gap analysis |
| TestGeneralizer 2026 | Scenario coverage via LLM inference | Probabilistic not deterministic |
| CloneDeMocker 2025 | Static Mockito deduplication | No scenario generation |
| Diffblue Cover | Structural gap analysis + test generation | No mock combination matrix |
| Parasoft Jtest | Runtime coverage + mock boilerplate | Execution dependent |
| EvoSuite | Search-based test generation | No Mockito awareness, 2007-era |

---

## DSC Score Interpretation Guide

```
0-30%    Critical — major scenario gaps, high production risk
30-60%   Insufficient — common paths tested, edge cases missing
60-80%   Acceptable — most scenarios covered, boundary cases remain
80-90%   Good — strong scenario coverage, review INFO items
90-100%  Excellent — comprehensive, focus on PIT for assertion strength
```

Use alongside:
```
JaCoCo line coverage > 80%     baseline execution coverage
SonarQube branch coverage > 70% decision path coverage
DSC score > 70%                scenario completeness
PIT mutation score > 70%       assertion strength (unit tests only)
```
