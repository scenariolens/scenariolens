# Contributing to ScenarioLens

Thank you for your interest in contributing! Phase 1 is complete and the core architecture is stable. The best places to contribute right now are listed in [`ROADMAP.md`](ROADMAP.md).

---

## Table of Contents

- [Getting Started](#getting-started)
- [Running the Test Suite](#running-the-test-suite)
- [Making Changes](#making-changes)
- [Commit Rules](#commit-rules)
- [Opening a Pull Request](#opening-a-pull-request)
- [Code Style](#code-style)

---

## Getting Started

```bash
git clone https://github.com/scenariolens/scenariolens.git
cd scenariolens

# Build everything (skips tests for speed on first clone)
mvn install -DskipTests -q

# Verify the example still works
cd examples/payment-service
mvn scenariolens:analyze -DtargetPackage=com.example.payment
# → target/scenariolens/report.html
```

Java 11+ and Maven 3.8+ are required.

---

## Running the Test Suite

**Always run the full suite before committing.** A pre-commit hook is included that does this automatically — but you can also run it manually at any time:

```bash
# From the repo root — runs all unit, regression, and load tests
mvn clean test -pl scenariolens-core,scenariolens-maven-plugin
```

Expected output on a green run:

```
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test breakdown

| Class | Suite | Count | What it covers |
|---|---|---|---|
| `PathPrunerTest` | Unit | 3 | CFG path pruning rules for `processRefund`, `getRefundPolicy`, `transfer` |
| `EdgeCaseTest` | Unit | 5 | No-call methods, switch mutual exclusivity, duplicate call indices, try/catch paths, compound null checks |
| `ScenarioMatrixRegressionTest` | Regression | 5 | `maxScenariosPerMethod` default=500, custom cap, `isTruncated()` semantics, sequential IDs, state reset between calls |
| `ScenarioMatrixLoadTest` | Load | 5 | 50k combinatorial guard (no OOM), output cap on 149-scenario method, < 2s per complex method, 50-iteration throughput < 5s, deterministic output across runs |

### Pre-commit hook

The hook at `.git/hooks/pre-commit` runs `mvn clean test` automatically before every `git commit`. It is already installed and executable in the repo. If it fires and fails, your commit is aborted — fix the failures, then retry.

> **Note:** The hook only runs locally. CI should be configured separately to run the same command.

---

## Making Changes

1. **Open an issue first** for anything non-trivial so we can align before you invest time.
2. Work on a branch: `git checkout -b feat/your-feature-name`
3. Keep commits small and focused — one logical change per commit.
4. Run the full test suite before pushing (the pre-commit hook handles this for local commits).
5. Add or update tests for every behaviour change:
   - Bug fix → add a regression test that would have caught the bug.
   - New feature → add unit tests covering the happy path and edge cases.

### Where to add tests

```
scenariolens-core/src/test/java/io/scenariolens/
├── cfg/
│   ├── EdgeCaseTest.java          ← edge cases in CFG / pruning
│   └── PathPrunerTest.java        ← pruning correctness per method
└── matrix/
    ├── ScenarioMatrixLoadTest.java      ← load + performance
    └── ScenarioMatrixRegressionTest.java ← cap behaviour regressions
```

---

## Commit Rules

Use [Conventional Commits](https://www.conventionalcommits.org/) prefixes:

| Prefix | Use for |
|---|---|
| `feat:` | New behaviour visible to users |
| `fix:` | Bug fix |
| `test:` | Adding or fixing tests only |
| `docs:` | README, CONTRIBUTING, ROADMAP changes |
| `refactor:` | Internal restructuring with no behaviour change |
| `perf:` | Performance improvements |

Example: `fix: prevent isTruncated() persisting across generate() calls`

---

## Opening a Pull Request

1. Push your branch and open a PR against `main`.
2. Describe **what** changed and **why** — reference the issue if one exists.
3. Confirm the test suite passes (`mvn clean test` output in the PR description is appreciated).
4. A maintainer will review within a few days.

---

## Code Style

- **Java 11** language level — no records, sealed classes, or text blocks yet.
- Standard Maven project layout.
- No framework dependencies in `scenariolens-core` beyond JavaParser and JUnit 5.
- Prefer clarity over cleverness — this is a developer tool that contributors need to reason about quickly.
- Keep new classes small and focused (the existing `CfgBuilder`, `PathPruner`, `ReturnVariationEnumerator` are good references).
